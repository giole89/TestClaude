package com.finai.dto.quote;

/**
 * Singolo punto OHLCV dello storico prezzi.
 * Usato sia nell'endpoint {@code GET /api/history/:ticker} che
 * incorporato in {@link FullQuoteDto}.
 */
public record HistoryPoint(
        String date,   // formato ISO "yyyy-MM-dd"
        Double open,
        Double high,
        Double low,
        Double close,
        Long   volume
) {}
