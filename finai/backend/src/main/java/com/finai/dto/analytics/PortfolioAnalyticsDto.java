package com.finai.dto.analytics;

import java.util.List;

/** Analisi aggregata del portafoglio. */
public record PortfolioAnalyticsDto(
        Double totalValue,
        Double totalCost,
        Double totalGainAmount,
        Double totalGainPct,
        /** Numero di posizioni attive. */
        int positionCount,
        /** Ticker con performance migliore. */
        String bestTicker,
        Double bestGainPct,
        /** Ticker con performance peggiore. */
        String worstTicker,
        Double worstGainPct,
        /** Posizione con peso maggiore nel portafoglio. */
        String topWeightTicker,
        Double topWeightPct,
        /** HHI (Herfindahl-Hirschman Index) 0–10000: <1500 diversificato, >2500 concentrato. */
        Double concentrationHhi,
        /** Numero di divise uniche nel portafoglio. */
        int currencyCount,
        List<PositionStats> positions
) {}
