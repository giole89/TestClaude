package com.finai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finai.dto.earnings.EarningsDto;
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
 * Recupera le date di earnings tramite l'API Yahoo Finance.
 *
 * <p>Yahoo Finance include nei dati di quote i campi {@code earningsTimestampStart}
 * e {@code earningsTimestampEnd} che rappresentano la finestra del prossimo earnings call.
 * Vengono anche estratti {@code epsForward} e {@code forwardPE}.</p>
 */
@Service
public class EarningsService {

    private static final Logger log = LoggerFactory.getLogger(EarningsService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final YahooCrumbProvider crumb;
    private final ObjectMapper mapper;

    @Value("${finai.yahoo.base-url-v7:https://query1.finance.yahoo.com/v7/finance}")
    private String baseUrlV7;

    public EarningsService(YahooCrumbProvider crumb, ObjectMapper mapper) {
        this.crumb  = crumb;
        this.mapper = mapper;
    }

    /**
     * Recupera i dati di earnings per una lista di ticker.
     *
     * @param tickers lista di simboli Yahoo Finance
     * @return lista di EarningsDto (ticker senza dati sono omessi)
     */
    @Cacheable(value = "earnings", key = "#tickers.toString()")
    @Retry(name = "yahooFinance")
    @CircuitBreaker(name = "yahooFinance", fallbackMethod = "earningsFallback")
    public List<EarningsDto> fetchEarnings(List<String> tickers) {
        if (tickers.isEmpty()) return List.of();

        String symbols = String.join(",", tickers);
        String url = baseUrlV7 + "/quote?symbols=" + symbols +
                "&fields=earningsTimestampStart,earningsTimestampEnd," +
                "epsForward,epsTrailingTwelveMonths,forwardPE," +
                "longName,shortName,regularMarketPrice";

        List<EarningsDto> result = new ArrayList<>();
        try {
            String body = crumb.fetch(url);
            JsonNode root = mapper.readTree(body);
            JsonNode rows = root.path("quoteResponse").path("result");

            if (!rows.isArray()) return result;

            for (JsonNode n : rows) {
                EarningsDto dto = mapToEarningsDto(n);
                if (dto != null) result.add(dto);
            }
        } catch (Exception e) {
            log.warn("Errore fetch earnings: {}", e.getMessage());
        }
        return result;
    }

    @SuppressWarnings("unused")
    private List<EarningsDto> earningsFallback(List<String> tickers, Throwable t) {
        log.warn("Circuit breaker aperto per earnings: {}", t.getMessage());
        return List.of();
    }

    // ── Parsing ────────────────────────────────────────────────────────────────

    private EarningsDto mapToEarningsDto(JsonNode n) {
        String ticker = n.path("symbol").asText(null);
        if (ticker == null) return null;

        Long earningsStart = nullableLong(n, "earningsTimestampStart");
        Long earningsEnd   = nullableLong(n, "earningsTimestampEnd");

        // Salta i ticker senza data earnings futura
        if (earningsStart == null && earningsEnd == null) return null;

        String earningsDate = formatTimestamp(earningsStart != null ? earningsStart : earningsEnd);

        String companyName = n.path("longName").asText(n.path("shortName").asText(ticker));
        Double epsForward  = nullableDouble(n, "epsForward");
        Double epsTrailing = nullableDouble(n, "epsTrailingTwelveMonths");
        Double forwardPE   = nullableDouble(n, "forwardPE");
        String quarter     = earningsDate != null ? deriveQuarter(earningsDate) : null;

        return new EarningsDto(
                ticker, companyName,
                earningsStart != null ? earningsStart * 1000L : null,
                earningsEnd   != null ? earningsEnd   * 1000L : null,
                earningsDate,
                epsForward, epsTrailing, forwardPE, quarter
        );
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

    private String deriveQuarter(String date) {
        try {
            LocalDate d = LocalDate.parse(date, DATE_FMT);
            int q = (d.getMonthValue() - 1) / 3 + 1;
            return "Q" + q + " " + d.getYear();
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
