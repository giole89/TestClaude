package com.finai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.finai.dto.ipo.RecentIpoDto;
import com.finai.dto.ipo.UpcomingIpoDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Accesso al calendario IPO pubblico di NASDAQ.
 *
 * <p>L'endpoint NASDAQ richiede header browser-specifici (Referer, Origin)
 * configurati in {@link com.finai.config.WebClientConfig#nasdaqWebClient}.
 * Protetto da retry e circuit breaker come Yahoo Finance.</p>
 */
@Service
public class NasdaqService {

    private static final Logger log = LoggerFactory.getLogger(NasdaqService.class);
    private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final WebClient nasdaqClient;

    @Value("${finai.nasdaq.base-url:https://api.nasdaq.com/api/ipo}")
    private String baseUrl;

    public NasdaqService(@Qualifier("nasdaqWebClient") WebClient nasdaqClient) {
        this.nasdaqClient = nasdaqClient;
    }

    /**
     * Recupera le prossime IPO dal calendario NASDAQ del mese corrente e successivo.
     *
     * @return lista di IPO in arrivo, lista vuota in caso di errore
     */
    @Cacheable("ipo")
    @Retry(name = "nasdaq")
    @CircuitBreaker(name = "nasdaq", fallbackMethod = "upcomingFallback")
    public List<UpcomingIpoDto> fetchUpcoming() {
        String date = LocalDate.now().format(YM_FMT);
        log.info("Fetching upcoming IPO per {}", date);

        JsonNode root = nasdaqClient.get()
                .uri(baseUrl + "/calendar?date=" + date)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        return parseUpcoming(root);
    }

    @SuppressWarnings("unused")
    private List<UpcomingIpoDto> upcomingFallback(Throwable t) {
        log.warn("NASDAQ circuit breaker aperto per upcoming: {}", t.getMessage());
        return List.of();
    }

    /**
     * Recupera le IPO recenti (ultimi 30 giorni).
     *
     * @return lista di IPO recenti con performance rispetto al prezzo IPO
     */
    @Cacheable(value = "ipo", key = "'recent'")
    @Retry(name = "nasdaq")
    @CircuitBreaker(name = "nasdaq", fallbackMethod = "recentFallback")
    public List<RecentIpoDto> fetchRecent() {
        // Il calendario NASDAQ del mese precedente contiene le IPO già quotate
        String date = LocalDate.now().minusMonths(1).format(YM_FMT);
        log.info("Fetching recent IPO per {}", date);

        JsonNode root = nasdaqClient.get()
                .uri(baseUrl + "/calendar?date=" + date)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        return parseRecent(root);
    }

    @SuppressWarnings("unused")
    private List<RecentIpoDto> recentFallback(Throwable t) {
        log.warn("NASDAQ circuit breaker aperto per recent: {}", t.getMessage());
        return List.of();
    }

    // ─────────────────────────────────── Parsing ─────────────────────────────

    private List<UpcomingIpoDto> parseUpcoming(JsonNode root) {
        List<UpcomingIpoDto> result = new ArrayList<>();
        if (root == null) return result;

        // Struttura NASDAQ: data.upcoming.upcomingTable.rows[]
        JsonNode rows = root.path("data").path("upcoming").path("upcomingTable").path("rows");
        if (!rows.isArray()) {
            // Fallback: struttura alternativa
            rows = root.path("data").path("rows");
        }

        if (rows.isArray()) {
            for (JsonNode row : rows) {
                result.add(new UpcomingIpoDto(
                        UUID.randomUUID().toString(),
                        row.path("companyName").asText(row.path("name").asText("")),
                        row.path("proposedTickerSymbol").asText(row.path("symbol").asText("")),
                        row.path("expectedPriceDate").asText(row.path("expectedDate").asText("")),
                        row.path("proposedSharePrice").asText(row.path("priceRange").asText("")),
                        row.path("sharesOffered").asText(""),
                        row.path("exchange").asText("NASDAQ"),
                        row.path("dealStatus").asText("")
                ));
            }
        }
        return result;
    }

    private List<RecentIpoDto> parseRecent(JsonNode root) {
        List<RecentIpoDto> result = new ArrayList<>();
        if (root == null) return result;

        JsonNode rows = root.path("data").path("recent").path("recentTable").path("rows");
        if (!rows.isArray()) {
            rows = root.path("data").path("priced").path("rows");
        }

        if (rows.isArray()) {
            for (JsonNode row : rows) {
                String priceStr = row.path("proposedSharePrice").asText("0")
                        .replaceAll("[^0-9.]", "");
                Double ipoPrice = priceStr.isBlank() ? null : Double.parseDouble(priceStr);

                result.add(new RecentIpoDto(
                        UUID.randomUUID().toString(),
                        row.path("companyName").asText(row.path("name").asText("")),
                        row.path("proposedTickerSymbol").asText(row.path("symbol").asText("")),
                        row.path("pricedDate").asText(row.path("ipoDate").asText("")),
                        ipoPrice,
                        null,    // currentPrice: aggiornato dal frontend via /api/quote
                        null,    // performance: calcolata quando currentPrice disponibile
                        row.path("exchange").asText("NASDAQ")
                ));
            }
        }
        return result;
    }
}
