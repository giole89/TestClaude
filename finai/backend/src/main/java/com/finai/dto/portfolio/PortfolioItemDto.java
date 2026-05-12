package com.finai.dto.portfolio;

import com.finai.domain.entity.PortfolioItem;

import java.time.Instant;

/**
 * DTO di risposta per una posizione del portafoglio.
 * Include il gain/loss calcolato se {@code currentPrice} è disponibile.
 */
public record PortfolioItemDto(
        String  id,
        String  ticker,
        String  name,
        Double  qty,
        Double  loadPrice,
        Double  currentPrice,
        String  currency,
        Instant createdAt
) {

    /** Costruisce il DTO dall'entità JPA. */
    public static PortfolioItemDto from(PortfolioItem e) {
        return new PortfolioItemDto(
                e.getId(),
                e.getTicker(),
                e.getName(),
                e.getQty()          != null ? e.getQty().doubleValue()          : null,
                e.getLoadPrice()    != null ? e.getLoadPrice().doubleValue()     : null,
                e.getCurrentPrice() != null ? e.getCurrentPrice().doubleValue()  : null,
                e.getCurrency(),
                e.getCreatedAt()
        );
    }
}
