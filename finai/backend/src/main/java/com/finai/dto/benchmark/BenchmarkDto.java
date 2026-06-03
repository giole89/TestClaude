package com.finai.dto.benchmark;

/** Confronto rendimento portafoglio vs S&P 500. */
public record BenchmarkDto(
        /** Rendimento portafoglio nel periodo (es. 12.5 = +12.5%). */
        Double portfolioReturn,
        /** Rendimento S&P 500 nel periodo. */
        Double benchmarkReturn,
        /** Alpha = portfolioReturn - benchmarkReturn. */
        Double alpha,
        /** Periodo es. "1y", "6m", "3m". */
        String period,
        /** Etichetta portafoglio. */
        String portfolioLabel,
        /** Etichetta benchmark. */
        String benchmarkLabel
) {}
