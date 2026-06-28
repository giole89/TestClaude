package com.finai.dto.finance;

/** Sintesi dell'andamento di mercato del giorno, usata per contestualizzare il portafoglio suggerito. */
public record MarketSnapshotDto(
        Double sp500ChangePct,
        Double vixLevel,
        String sentiment,
        String note
) {}
