package com.finai.dto.watchlist;

/** Request per aggiungere un ticker alla watchlist. */
public record AddWatchlistRequest(
        String id,
        String ticker,
        String name,
        Double targetPrice,
        String note
) {}
