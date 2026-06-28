package com.finai.dto.simulator;

import com.finai.domain.entity.SimPosition;

import java.time.Instant;

/**
 * DTO di risposta per una posizione simulata, con valorizzazione live.
 *
 * <p>{@code currentPrice} è null se Yahoo Finance non ha restituito una quote
 * per il ticker (es. servizio temporaneamente non disponibile): in tal caso
 * {@code value}/{@code pnl}/{@code pnlPct} ricadono sul prezzo medio di carico.</p>
 */
public record SimPositionDto(
        String  id,
        String  ticker,
        String  name,
        Double  qty,
        Double  avgPrice,
        Double  currentPrice,
        String  currency,
        Double  value,
        Double  pnl,
        Double  pnlPct,
        Instant createdAt
) {

    public static SimPositionDto of(SimPosition e, Double currentPrice) {
        double qty = e.getQty().doubleValue();
        double avgPrice = e.getAvgPrice().doubleValue();
        double refPrice = currentPrice != null ? currentPrice : avgPrice;
        double value = qty * refPrice;
        double cost = qty * avgPrice;
        double pnl = value - cost;
        double pnlPct = cost > 0 ? (pnl / cost) * 100.0 : 0.0;

        return new SimPositionDto(
                e.getId(), e.getTicker(), e.getName(), qty, avgPrice,
                currentPrice, e.getCurrency(), value, pnl, pnlPct, e.getCreatedAt()
        );
    }
}
