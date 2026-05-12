package com.finai.dto.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

/**
 * Corpo della richiesta {@code POST /api/ai/chat}.
 *
 * <p>{@code context} è una mappa flessibile che il frontend popola con i dati
 * contestuali della tab attiva (ticker, portafoglio, ecc.). Il backend la usa
 * per costruire il system prompt specializzato.</p>
 */
public record ChatRequest(
        @Valid @NotEmpty(message = "messages non può essere vuoto")
        List<ChatMessage> messages,

        /** Contesto opzionale: tab, ticker, tickerData, portfolioItems, ecc. */
        Map<String, Object> context
) {}
