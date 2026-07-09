package com.finai.dto.finance.mortgage;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Corpo della richiesta di simulazione di un finanziamento/prestito personale.
 *
 * @param monthlyNetIncome reddito netto mensile dichiarato dall'utente; se null, si usa la stima
 *                         calcolata dal budget (media entrate dagli estratti conto importati)
 */
public record LoanRequest(
        @NotNull(message = "loanAmount obbligatorio")
        @Positive(message = "loanAmount deve essere positivo")
        Double loanAmount,

        @NotNull(message = "interestRatePct obbligatorio")
        @Positive(message = "interestRatePct deve essere positivo")
        Double interestRatePct,

        @NotNull(message = "months obbligatorio")
        @Positive(message = "months deve essere positivo")
        @Max(value = 180, message = "months non può superare 180 (15 anni)")
        Integer months,

        Double monthlyNetIncome
) {}
