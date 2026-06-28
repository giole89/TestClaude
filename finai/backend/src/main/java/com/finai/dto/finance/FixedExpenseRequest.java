package com.finai.dto.finance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Corpo della richiesta di creazione/aggiornamento di una spesa fissa. */
public record FixedExpenseRequest(
        @NotBlank(message = "name obbligatorio")
        @Size(max = 100)
        String name,

        @NotBlank(message = "category obbligatoria")
        @Size(max = 50)
        String category,

        @NotNull(message = "amount obbligatorio")
        @Positive(message = "amount deve essere positivo")
        Double amount,

        Boolean active
) {}
