package com.finai.dto.finance.mortgage;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Corpo della richiesta di simulazione di un mutuo.
 *
 * @param monthlyNetIncome reddito netto mensile dichiarato dall'utente; se null, si usa la stima
 *                         calcolata dal budget (media entrate dagli estratti conto importati)
 */
public record MortgageRequest(
        @NotNull(message = "propertyValue obbligatorio")
        @Positive(message = "propertyValue deve essere positivo")
        Double propertyValue,

        @NotNull(message = "loanAmount obbligatorio")
        @Positive(message = "loanAmount deve essere positivo")
        Double loanAmount,

        @NotNull(message = "interestRatePct obbligatorio")
        @Positive(message = "interestRatePct deve essere positivo")
        Double interestRatePct,

        @NotNull(message = "years obbligatorio")
        @Positive(message = "years deve essere positivo")
        @Max(value = 50, message = "years non può superare 50")
        Integer years,

        Double monthlyNetIncome
) {}
