package com.finai.dto.quote;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Quote base di un titolo finanziario.
 * Corrisponde alla risposta di {@code GET /api/quote/:ticker}.
 */
@Schema(description = "Quote base di un titolo")
public record QuoteDto(
        @Schema(description = "Simbolo Yahoo Finance") String ticker,
        @Schema(description = "Nome completo")         String name,
        @Schema(description = "Prezzo corrente")       Double price,
        @Schema(description = "Variazione assoluta")   Double dayChange,
        @Schema(description = "Variazione percentuale")Double dayChangePct,
        @Schema(description = "Variazione % da inizio anno (YTD)") Double ytdChangePct,
        @Schema(description = "Massimo 52 settimane")  Double high52w,
        @Schema(description = "Minimo 52 settimane")   Double low52w,
        @Schema(description = "Volume giornaliero")    Long   volume,
        @Schema(description = "Capitalizzazione")      Long   marketCap,
        @Schema(description = "P/E ratio")             Double pe,
        @Schema(description = "Valuta (es. USD, EUR)") String currency,
        @Schema(description = "Exchange (es. NasdaqGS)")String exchange,
        /**
         * Posizione del prezzo corrente nel range 52 settimane: 0 = minimo, 100 = massimo.
         * Utile per visualizzare barre di range senza calcoli lato frontend.
         */
        @Schema(description = "Posizione 0-100 nel range 52W") Integer rangePosition,
        @Schema(description = "Timestamp Unix ms") Long timestamp
) {}
