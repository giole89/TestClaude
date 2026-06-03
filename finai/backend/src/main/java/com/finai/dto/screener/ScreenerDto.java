package com.finai.dto.screener;

/** Dati di screening per un titolo azionario o ETF. */
public record ScreenerDto(
        String ticker,
        String name,
        Double price,
        Double pe,
        Double dividendYield,
        Double ytdChangePct,
        Double high52w,
        Double low52w,
        Long marketCap,
        String exchange,
        /** Posizione nel range 52w (0-100). */
        Integer rangePosition,
        String currency
) {}
