package com.finai.dto.ipo;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Corpo della richiesta {@code POST /api/ipo/watchlist}. */
public record AddIpoWatchlistRequest(
        @NotBlank String id,

        @Size(max = 20) String ticker,

        @NotBlank String companyName,

        LocalDate expectedDate,
        String    exchange,
        String    sector,

        @Positive Integer lockupDays,

        @Positive BigDecimal ipoPrice,

        String notes
) {}
