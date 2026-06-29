package com.finai.dto.finance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Correzione manuale di categoria e/o tipo di un movimento importato, per i casi non riconosciuti automaticamente. */
public record TransactionUpdateRequest(
        @NotBlank String category,
        @Pattern(regexp = "INCOME|VARIABLE_EXPENSE", message = "type deve essere INCOME o VARIABLE_EXPENSE") @NotBlank String type
) {}
