package com.finai.dto.simulator;

import java.util.List;

/**
 * Riepilogo aggregato dell'ambiente di simulazione: liquidità, posizioni
 * valorizzate live e performance complessiva rispetto al capitale iniziale.
 */
public record SimSummaryDto(
        Double cashBalance,
        Double startingBalance,
        Double positionsValue,
        Double totalValue,
        Double totalPnl,
        Double totalPnlPct,
        List<SimPositionDto> positions
) {}
