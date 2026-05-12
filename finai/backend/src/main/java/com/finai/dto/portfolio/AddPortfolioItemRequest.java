package com.finai.dto.portfolio;

import jakarta.validation.constraints.*;

/**
 * Corpo della richiesta {@code POST /api/portfolio}.
 * L'ID è generato lato client (UUID v4) per permettere operazioni ottimistiche nel frontend.
 */
public record AddPortfolioItemRequest(
        @NotBlank(message = "id obbligatorio")
        String id,

        @NotBlank(message = "ticker obbligatorio")
        @Size(max = 20)
        String ticker,

        @NotBlank(message = "name obbligatorio")
        String name,

        @NotNull(message = "qty obbligatoria")
        @Positive(message = "qty deve essere positiva")
        Double qty,

        @NotNull(message = "loadPrice obbligatorio")
        @Positive(message = "loadPrice deve essere positivo")
        Double loadPrice,

        String currency
) {}
