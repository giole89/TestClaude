package com.finai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finai.dto.dividend.DividendDto;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Recupera i dati dividendi tramite Yahoo Finance API v7.
 *
 * <p>Utilizza i campi {@code trailingAnnualDividendRate}, {@code trailingAnnualDividendYield},
 * {@code dividendDate} e {@code exDividendDate} dalla quote API.</p>
 */
@Service
public class DividendService {

    private static final Logger log = LoggerFactory.getLogger(DividendService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final YahooCrumbProvider crumb;
    private final ObjectMapper mapper;

    @Value("${finai.yahoo.base-url-v7:https://query1.finance.yahoo.com/v7/finance}")
    private String baseUrlV7;

    public DividendService(YahooCrumbProvider crumb, ObjectMapper mapper) {
        this.crumb  = crumb;
        this.mapper = mapper;
    }

    /**
     * Recupera i dati dividendi per una lista di ticker.
     * Solo i ticker con dividendo positivo vengono restituiti.
     *
     * @param tickers lista di simboli Yahoo Finance
     * @return lista di DividendDto (ticker senza dividendi omessi)
     */
    @Cacheable(value = "dividends", key = "#tickers.toString()")
    @Retry(name = "yahooFinance")
    @CircuitBreaker(name = "yahooFinance", fallbackMethod = "dividendsFallback")
    public List<DividendDto> fetchDividends(List<String> tickers) {
        if (tickers == null || tickers.isEmpty()) return List.of();

        String symbols = String.join(",", tickers);
        String url = baseUrlV7 + "/quote?symbols=" + symbols +
                "&fields=trailingAnnualDividendRate,trailingAnnualDividendYield," +
                "dividendDate,exDividendDate,longName,shortName";

        List<DividendDto> result = new ArrayList<>();
        try {
            String body = crumb.fetch(url);
            JsonNode root = mapper.readTree(body);
            JsonNode rows = root.path("quoteResponse").path("result");

            if (!rows.isArray()) return result;

            for (JsonNode n : rows) {
                DividendDto dto = mapToDividendDto(n);
                if (dto != null) result.add(dto);
            }
        } catch (Exception e) {
            log.warn("Errore fetch dividendi: {}", e.getMessage());
        }
        return result;
    }

    @SuppressWarnings("unused")
    private List<DividendDto> dividendsFallback(List<String> tickers, Throwable t) {
        log.warn("Circuit breaker aperto per dividendi: {}", t.getMessage());
        return List.of();
    }

    // ── Parsing ────────────────────────────────────────────────────────────────

    private DividendDto mapToDividendDto(JsonNode n) {
        String ticker = n.path("symbol").asText(null);
        if (ticker == null) return null;

        Double annualDividend = nullableDouble(n, "trailingAnnualDividendRate");
        Double dividendYield  = nullableDouble(n, "trailingAnnualDividendYield");

        // Filtra ticker senza dividendi
        if (annualDividend == null || annualDividend <= 0) return null;

        String companyName   = n.path("longName").asText(n.path("shortName").asText(ticker));
        String exDividendDate = formatTimestamp(nullableLong(n, "exDividendDate"));
        String dividendDate   = formatTimestamp(nullableLong(n, "dividendDate"));

        Integer payFrequency = estimateFrequency(annualDividend, dividendYield);

        return new DividendDto(
                ticker, companyName, annualDividend, dividendYield,
                exDividendDate, dividendDate, payFrequency
        );
    }

    /**
     * Stima la frequenza di pagamento dividendi.
     * Senza dati storici, usiamo una heuristica basata sul tipo di mercato:
     * - yield molto alto (>6%) → mensile
     * - yield alto (>4%) → trimestrale
     * - default → trimestrale (US) / annuale
     */
    private Integer estimateFrequency(Double annualRate, Double dividendYield) {
        if (annualRate == null || dividendYield == null) return 4;
        // Approssimazione: se il dividendo annuale non è divisibile per importi standard
        // usiamo una euristica semplice
        if (dividendYield != null && dividendYield > 0.06) return 12; // mensile (REIT/BDC)
        return 4; // default trimestrale
    }

    private String formatTimestamp(Long unixSeconds) {
        if (unixSeconds == null) return null;
        try {
            LocalDate date = Instant.ofEpochSecond(unixSeconds).atZone(ZoneOffset.UTC).toLocalDate();
            return date.format(DATE_FMT);
        } catch (Exception e) {
            return null;
        }
    }

    private Double nullableDouble(JsonNode n, String field) {
        JsonNode v = n.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asDouble();
    }

    private Long nullableLong(JsonNode n, String field) {
        JsonNode v = n.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asLong();
    }
}
