package com.finai.dto.simulator;

import jakarta.validation.constraints.Positive;

/**
 * Corpo opzionale della richiesta {@code POST /api/sim/reset}.
 * Se {@code startingBalance} è null, viene riusato il capitale iniziale precedente.
 */
public record ResetRequest(
        @Positive(message = "startingBalance deve essere positivo")
        Double startingBalance
) {}
