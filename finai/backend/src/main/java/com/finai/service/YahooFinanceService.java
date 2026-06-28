package com.finai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finai.dto.quote.HistoryPoint;
import com.finai.dto.quote.QuoteDto;
import com.finai.dto.search.SearchResultDto;
import com.finai.exception.FinaiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Servizio di accesso a Yahoo Finance.
 *
 * <p>Utilizza {@link YahooCrumbProvider} per gestire l'autenticazione
 * (crumb token + cookie) richiesta da Yahoo Finance dal 2024.</p>
 *
 * <p>Tutte le chiamate esterne sono protette da:
 * <ul>
 *   <li>{@link Retry} — 3 tentativi con backoff esponenziale 2s→4s→8s</li>
 *   <li>{@link CircuitBreaker} — si apre dopo 50% di errori su 10 chiamate,
 *       si resetta dopo 60s</li>
 * </ul>
 *
 * <p>I risultati sono cachati in memoria (Caffeine) con TTL configurati in
 * {@code CacheConfig}.</p>
 */
@Service
public class YahooFinanceService {

    private static final Logger log = LoggerFactory.getLogger(YahooFinanceService.class);

    private final YahooCrumbProvider crumb;
    private final IndicatorsService   indicators;
    private final ObjectMapper        mapper;

    @Value("${finai.yahoo.base-url-v7:https://query1.finance.yahoo.com/v7/finance}")
    private String baseUrlV7;

    @Value("${finai.yahoo.base-url-v8:https://query1.finance.yahoo.com/v8/finance}")
    private String baseUrlV8;

    @Value("${finai.yahoo.base-url-search:https://query1.finance.yahoo.com/v1/finance}")
    private String baseUrlSearch;

    public YahooFinanceService(YahooCrumbProvider crumb,
                               IndicatorsService indicators,
                               ObjectMapper mapper) {
        this.crumb      = crumb;
        this.indicators = indicators;
        this.mapper     = mapper;
    }

    // ─────────────────────────────────── Quote singola ───────────────────────

    /**
     * Recupera la quote base di un singolo ticker.
     *
     * @param ticker simbolo Yahoo Finance (es. "AAPL", "ISP.MI")
     * @return QuoteDto, oppure null se il circuit breaker è aperto
     */
    @Cacheable(value = "quotes", key = "#ticker.toUpperCase()")
    @Retry(name = "yahooFinance")
    @CircuitBreaker(name = "yahooFinance", fallbackMethod = "quoteFallback")
    public QuoteDto fetchQuote(String ticker) {
        log.debug("Fetching quote per {}", ticker);
        String url = baseUrlV7 + "/quote?symbols=" + encodeSymbol(ticker) +
                "&fields=regularMarketPrice,regularMarketChange,regularMarketChangePercent," +
                "regularMarketVolume,marketCap,trailingPE,fiftyTwoWeekHigh,fiftyTwoWeekLow," +
                "longName,shortName,fullExchangeName,currency,ytdReturn,fiftyTwoWeekChangePercent";

        JsonNode root = fetch(url);
        JsonNode result = extractFirstResult(root, "quoteResponse");
        return mapToQuoteDto(result);
    }

    @SuppressWarnings("unused")
    private QuoteDto quoteFallback(String ticker, Throwable t) {
        log.warn("Circuit breaker aperto per quote {}: {}", ticker, t.getMessage());
        return null;
    }

    // ─────────────────────────────────── Batch ───────────────────────────────

    /**
     * Recupera le quote di più ticker in una singola chiamata HTTP.
     *
     * @param tickers lista di simboli (max ~20 per chiamata Yahoo)
     * @return lista di QuoteDto (eventuali ticker non trovati vengono omessi)
     */
    @Cacheable(value = "batch", key = "#tickers.stream().sorted().collect(T(java.util.stream.Collectors).joining(','))")
    @Retry(name = "yahooFinance")
    @CircuitBreaker(name = "yahooFinance", fallbackMethod = "batchFallback")
    public List<QuoteDto> fetchBatch(List<String> tickers) {
        if (tickers == null || tickers.isEmpty()) return List.of();
        String symbols = tickers.stream().map(this::encodeSymbol).collect(java.util.stream.Collectors.joining(","));
        log.debug("Fetching batch per {} ticker: {}", tickers.size(), symbols);

        String url = baseUrlV7 + "/quote?symbols=" + symbols +
                "&fields=regularMarketPrice,regularMarketChange,regularMarketChangePercent," +
                "regularMarketVolume,marketCap,trailingPE,fiftyTwoWeekHigh,fiftyTwoWeekLow," +
                "longName,shortName,fullExchangeName,currency,ytdReturn,fiftyTwoWeekChangePercent";

        JsonNode root = fetch(url);
        List<QuoteDto> results = new ArrayList<>();
        JsonNode resultArray = root.path("quoteResponse").path("result");
        if (resultArray.isArray()) {
            for (JsonNode node : resultArray) {
                try { results.add(mapToQuoteDto(node)); }
                catch (Exception e) { log.debug("Skipping ticker in batch: {}", e.getMessage()); }
            }
        }
        return results;
    }

    @SuppressWarnings("unused")
    private List<QuoteDto> batchFallback(List<String> tickers, Throwable t) {
        log.warn("Circuit breaker aperto per batch: {}", t.getMessage());
        return List.of();
    }

    // ─────────────────────────────────── Storico ─────────────────────────────

    /**
     * Recupera lo storico OHLCV da Yahoo Finance Chart API (v8).
     *
     * @param ticker simbolo
     * @param range  periodo: "1m" | "3m" | "6m" | "1y"
     * @return lista di HistoryPoint in ordine cronologico
     */
    @Cacheable(value = "history", key = "#ticker.toUpperCase() + ':' + #range")
    @Retry(name = "yahooFinance")
    @CircuitBreaker(name = "yahooFinance", fallbackMethod = "historyFallback")
    public List<HistoryPoint> fetchHistory(String ticker, String range) {
        log.debug("Fetching history {} range={}", ticker, range);
        String url = baseUrlV8 + "/chart/" + encodeSymbol(ticker) + "?interval=1d&range=" + range;
        return parseHistory(fetch(url));
    }

    @SuppressWarnings("unused")
    private List<HistoryPoint> historyFallback(String ticker, String range, Throwable t) {
        log.warn("Circuit breaker aperto per history {} {}: {}", ticker, range, t.getMessage());
        return List.of();
    }

    // ─────────────────────────────────── Ricerca ─────────────────────────────

    /**
     * Ricerca autocomplete di ticker e nomi strumenti.
     *
     * @param query stringa di ricerca (min 2 caratteri)
     * @return lista di risultati (max 10)
     */
    @Cacheable(value = "search", key = "#query.toLowerCase()")
    @Retry(name = "yahooFinance")
    @CircuitBreaker(name = "yahooFinance", fallbackMethod = "searchFallback")
    public List<SearchResultDto> search(String query) {
        log.debug("Ricerca ticker: {}", query);
        String url = baseUrlSearch + "/search?q=" + query +
                "&quotesCount=10&lang=en-US&newsCount=0&enableFuzzyQuery=false";

        JsonNode root = fetch(url);
        List<SearchResultDto> results = new ArrayList<>();
        JsonNode quotes = root.path("quotes");
        if (quotes.isArray()) {
            for (JsonNode q : quotes) {
                String symbol    = q.path("symbol").asText(null);
                String longName  = q.path("longname").asText(null);
                String shortName = q.path("shortname").asText(null);
                String exchDisp  = q.path("exchDisp").asText(q.path("exchange").asText(""));
                String quoteType = q.path("quoteType").asText("EQUITY");
                if (symbol == null || symbol.isBlank()) continue;
                String name = longName != null ? longName : (shortName != null ? shortName : symbol);
                results.add(new SearchResultDto(symbol, name, exchDisp, quoteType));
            }
        }
        return results;
    }

    @SuppressWarnings("unused")
    private List<SearchResultDto> searchFallback(String query, Throwable t) {
        log.warn("Circuit breaker aperto per search {}: {}", query, t.getMessage());
        return List.of();
    }

    // ─────────────────────────────────── Parsing privato ─────────────────────

    /**
     * URL-encode di un simbolo. Necessario perché alcuni ticker Yahoo
     * contengono caratteri non validi in un URI grezzo (es. {@code ^GSPC},
     * {@code EURUSD=X}), che fanno fallire {@code URI.create()} con
     * {@code IllegalArgumentException: Illegal character}.
     */
    private String encodeSymbol(String symbol) {
        return java.net.URLEncoder.encode(symbol, java.nio.charset.StandardCharsets.UTF_8);
    }

    private JsonNode fetch(String url) {
        try {
            return mapper.readTree(crumb.fetch(url));
        } catch (Exception e) {
            throw new FinaiException("Errore chiamata Yahoo Finance: " + e.getMessage(), 502);
        }
    }

    private QuoteDto mapToQuoteDto(JsonNode n) {
        if (n == null || n.isMissingNode()) throw new FinaiException("Ticker non trovato", 404);

        String ticker    = n.path("symbol").asText();
        String longName  = n.path("longName").asText(n.path("shortName").asText(ticker));
        Double price     = nullableDouble(n, "regularMarketPrice");
        Double change    = nullableDouble(n, "regularMarketChange");
        Double changePct = nullableDouble(n, "regularMarketChangePercent");
        // ytdReturn disponibile per ETF/fondi; fiftyTwoWeekChangePercent come fallback per azioni
        Double ytd       = nullableDouble(n, "ytdReturn");
        if (ytd == null) {
            Double w52chg = nullableDouble(n, "fiftyTwoWeekChangePercent");
            if (w52chg != null) ytd = w52chg * 100.0;
        } else {
            ytd = ytd * 100.0; // Yahoo restituisce ytdReturn come decimale (es. 0.12 = 12%)
        }
        Double high52w   = nullableDouble(n, "fiftyTwoWeekHigh");
        Double low52w    = nullableDouble(n, "fiftyTwoWeekLow");
        Long   volume    = nullableLong(n, "regularMarketVolume");
        Long   mktCap    = nullableLong(n, "marketCap");
        Double pe        = nullableDouble(n, "trailingPE");
        String currency  = n.path("currency").asText("USD");
        String exchange  = n.path("fullExchangeName").asText("");

        Integer rangePos = indicators.calcRangePosition(price, low52w, high52w);

        return new QuoteDto(ticker, longName, price, change, changePct, ytd,
                high52w, low52w, volume, mktCap, pe, currency, exchange,
                rangePos, Instant.now().toEpochMilli());
    }

    private List<HistoryPoint> parseHistory(JsonNode root) {
        JsonNode result = root.path("chart").path("result");
        if (!result.isArray() || result.isEmpty()) return List.of();

        JsonNode data       = result.get(0);
        JsonNode timestamps = data.path("timestamp");
        JsonNode quote      = data.path("indicators").path("quote").path(0);

        if (!timestamps.isArray()) return List.of();

        List<HistoryPoint> points = new ArrayList<>();
        for (int i = 0; i < timestamps.size(); i++) {
            long ts = timestamps.get(i).asLong();
            LocalDate date  = Instant.ofEpochSecond(ts).atZone(ZoneOffset.UTC).toLocalDate();
            Double open     = safeArrayDouble(quote.path("open"),   i);
            Double high     = safeArrayDouble(quote.path("high"),   i);
            Double low      = safeArrayDouble(quote.path("low"),    i);
            Double close    = safeArrayDouble(quote.path("close"),  i);
            Long   volume   = safeArrayLong(quote.path("volume"),   i);
            if (close != null) {
                points.add(new HistoryPoint(date.toString(), open, high, low, close, volume));
            }
        }
        return points;
    }

    private JsonNode extractFirstResult(JsonNode root, String key) {
        JsonNode arr = root.path(key).path("result");
        if (arr.isArray() && !arr.isEmpty()) return arr.get(0);
        return null;
    }

    private Double nullableDouble(JsonNode n, String field) {
        JsonNode v = n.path(field);
        return v.isNull() || v.isMissingNode() ? null : v.asDouble();
    }

    private Long nullableLong(JsonNode n, String field) {
        JsonNode v = n.path(field);
        return v.isNull() || v.isMissingNode() ? null : v.asLong();
    }

    private Double safeArrayDouble(JsonNode arr, int idx) {
        if (!arr.isArray() || idx >= arr.size()) return null;
        JsonNode v = arr.get(idx);
        return v.isNull() ? null : v.asDouble();
    }

    private Long safeArrayLong(JsonNode arr, int idx) {
        if (!arr.isArray() || idx >= arr.size()) return null;
        JsonNode v = arr.get(idx);
        return v.isNull() ? null : v.asLong();
    }
}
