package com.finai.dto.quote;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Quote completa con indicatori tecnici e storico prezzi.
 * Corrisponde alla risposta di {@code GET /api/quote/:ticker/full}.
 *
 * <p>Tutti gli indicatori sono calcolati server-side su {@code history}
 * tramite {@code IndicatorsService}.</p>
 */
@Schema(description = "Quote completa con indicatori tecnici e storico")
public record FullQuoteDto(
        // ── Dati base (stessi di QuoteDto) ────────────────────────────
        String  ticker,
        String  name,
        Double  price,
        Double  dayChange,
        Double  dayChangePct,
        Double  ytdChangePct,
        Double  high52w,
        Double  low52w,
        Long    volume,
        Long    marketCap,
        Double  pe,
        String  currency,
        String  exchange,
        Integer rangePosition,
        Long    timestamp,

        // ── Indicatori tecnici ─────────────────────────────────────────
        @Schema(description = "RSI a 14 periodi (0-100)") Integer rsi,
        @Schema(description = "SMA a 20 giorni")          Double  sma20,
        @Schema(description = "SMA a 50 giorni")          Double  sma50,
        @Schema(description = "SMA a 200 giorni")         Double  sma200,
        @Schema(description = "Volatilità annualizzata %")Integer volatility,
        @Schema(description = "Momentum a 30 giorni %")   Double  momentum30,
        @Schema(description = "BullScore composito 0-100")Integer bullScore,

        // ── Storico ───────────────────────────────────────────────────
        @Schema(description = "Storico OHLCV un anno") List<HistoryPoint> history
) {}
