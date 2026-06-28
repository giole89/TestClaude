package com.finai.dto.simulator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Corpo della richiesta {@code POST /api/sim/sell}. */
public record SellRequest(
        @NotBlank(message = "ticker obbligatorio")
        @Size(max = 20)
        String ticker,

        @NotNull(message = "qty obbligatoria")
        @Positive(message = "qty deve essere positiva")
        Double qty
) {}
