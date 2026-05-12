package com.finai.dto.ipo;

import com.finai.domain.entity.IpoWatchlistItem;

import java.time.Instant;
import java.time.LocalDate;

/** DTO di risposta per un elemento della watchlist IPO. */
public record IpoWatchlistItemDto(
        String    id,
        String    ticker,
        String    companyName,
        LocalDate expectedDate,
        String    exchange,
        String    sector,
        int       lockupDays,
        Double    ipoPrice,
        LocalDate ipoDate,
        /** Giorni rimanenti al termine del lock-up. Null se ipoDate non impostata. */
        Long      lockupRemainingDays,
        String    notes,
        Instant   createdAt
) {

    public static IpoWatchlistItemDto from(IpoWatchlistItem e) {
        return new IpoWatchlistItemDto(
                e.getId(),
                e.getTicker(),
                e.getCompanyName(),
                e.getExpectedDate(),
                e.getExchange(),
                e.getSector(),
                e.getLockupDays(),
                e.getIpoPrice() != null ? e.getIpoPrice().doubleValue() : null,
                e.getIpoDate(),
                e.lockupRemainingDays(),
                e.getNotes(),
                e.getCreatedAt()
        );
    }
}
