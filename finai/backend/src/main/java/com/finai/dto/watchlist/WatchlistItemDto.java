package com.finai.dto.watchlist;

import com.finai.domain.entity.WatchlistItem;

/** DTO per un elemento della watchlist. */
public record WatchlistItemDto(
        String id,
        String ticker,
        String name,
        Double targetPrice,
        String note,
        Long createdAt
) {
    public static WatchlistItemDto from(WatchlistItem item) {
        return new WatchlistItemDto(
                item.getId(),
                item.getTicker(),
                item.getName(),
                item.getTargetPrice() != null ? item.getTargetPrice().doubleValue() : null,
                item.getNote(),
                item.getCreatedAt()
        );
    }
}
