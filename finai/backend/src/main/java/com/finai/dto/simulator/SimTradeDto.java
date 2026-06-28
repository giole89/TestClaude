package com.finai.dto.simulator;

import com.finai.domain.entity.SimTrade;

import java.time.Instant;

/** DTO di risposta per una riga di storico operazioni simulate. */
public record SimTradeDto(
        String  id,
        String  ticker,
        String  name,
        String  side,
        Double  qty,
        Double  price,
        Double  amount,
        Double  realizedPnl,
        String  currency,
        Instant executedAt
) {

    public static SimTradeDto from(SimTrade e) {
        return new SimTradeDto(
                e.getId(), e.getTicker(), e.getName(), e.getSide(),
                e.getQty().doubleValue(), e.getPrice().doubleValue(), e.getAmount().doubleValue(),
                e.getRealizedPnl() != null ? e.getRealizedPnl().doubleValue() : null,
                e.getCurrency(), e.getExecutedAt()
        );
    }
}
