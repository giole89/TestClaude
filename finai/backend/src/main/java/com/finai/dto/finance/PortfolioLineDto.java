package com.finai.dto.finance;

/** Riga di un portafoglio concreto: strumento reale, peso e quota di mercato corrente. */
public record PortfolioLineDto(
        String ticker,
        String name,
        String assetClass,
        double weightPct,
        Double price,
        Double dayChangePct,
        String currency,
        String rationale
) {}
