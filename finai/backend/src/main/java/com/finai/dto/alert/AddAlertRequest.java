package com.finai.dto.alert;

import jakarta.validation.constraints.*;

/**
 * Corpo della richiesta {@code POST /api/alerts}.
 */
public record AddAlertRequest(
        @NotBlank String id,

        @NotBlank @Size(max = 20)
        String ticker,

        /** Valori accettati: above | below | change_up | change_down */
        @NotBlank @Pattern(regexp = "above|below|change_up|change_down",
                           message = "type deve essere: above, below, change_up, change_down")
        String type,

        @NotNull @Positive Double value
) {}
