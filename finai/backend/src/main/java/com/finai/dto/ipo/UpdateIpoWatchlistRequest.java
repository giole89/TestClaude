package com.finai.dto.ipo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Corpo della richiesta {@code PATCH /api/ipo/watchlist/:id}.
 * Tutti i campi sono opzionali: vengono aggiornati solo quelli non null.
 */
public record UpdateIpoWatchlistRequest(
        String     ticker,
        LocalDate  expectedDate,
        LocalDate  ipoDate,
        String     exchange,
        String     sector,
        Integer    lockupDays,
        BigDecimal ipoPrice,
        String     notes
) {}
