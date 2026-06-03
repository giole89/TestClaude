package com.finai.dto.analytics;

/** Metriche di una singola posizione nel portafoglio. */
public record PositionStats(
        String ticker,
        String name,
        Double qty,
        Double loadPrice,
        Double currentPrice,
        Double marketValue,
        Double cost,
        Double gainAmount,
        Double gainPct,
        /** Peso % sul totale portafoglio. */
        Double weight,
        /** Contributo in punti percentuali al P&L totale. */
        Double contributionPct,
        String currency
) {}
