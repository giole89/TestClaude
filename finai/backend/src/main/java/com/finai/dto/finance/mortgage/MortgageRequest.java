package com.finai.dto.finance.mortgage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Corpo della richiesta di simulazione di un mutuo per l'acquisto di una casa.
 *
 * @param monthlyNetIncome reddito netto mensile dichiarato dall'utente; se null, si usa la stima
 *                         calcolata dal budget (media entrate dagli estratti conto importati)
 * @param purchaseType     "PRIMA_CASA_PRIVATO" (default se null), "PRIMA_CASA_COSTRUTTORE",
 *                         "SECONDA_CASA_PRIVATO" o "SECONDA_CASA_COSTRUTTORE": determina l'aliquota
 *                         di imposta stimata se {@code registrationTax} non è dichiarata, e se
 *                         l'eventuale anticipazione del fondo pensione è ammessa (solo prima casa)
 * @param notaryCosts      costi di notaio dichiarati; se null, stima indicativa
 * @param originationFees  spese di istruttoria della banca dichiarate; se null, stima indicativa
 * @param appraisalFees    spese di perizia dell'immobile dichiarate; se null, stima indicativa
 * @param agencyFeePct     percentuale di commissione dell'agenzia immobiliare (esclusa IVA), se preferisci
 *                         indicarla in percentuale anziché in euro; l'IVA al 22% viene aggiunta automaticamente.
 *                         Ha priorità su {@code agencyFeeAmount} se entrambi sono valorizzati
 * @param agencyFeeAmount  spese di agenzia immobiliare dichiarate in euro, importo finale già comprensivo di
 *                         IVA (es. da preventivo dell'agenzia); usata solo se {@code agencyFeePct} è null.
 *                         Se nessuno dei due è valorizzato, stima indicativa (~3% + IVA del valore immobile)
 * @param registrationTax  imposta di registro/IVA sull'acquisto dichiarata (es. da preventivo notarile);
 *                         se null, stima indicativa basata su {@code purchaseType} (approssimata sul
 *                         prezzo dichiarato, non sul valore catastale realmente usato per calcolarla)
 * @param liquidSavings    liquidità disponibile per coprire capitale proprio e spese accessorie; se
 *                         null, si usa la liquidità dichiarata nel questionario investitore, se presente
 * @param pensionFundYears anni di iscrizione al fondo pensione complementare, se ne hai uno (opzionale;
 *                         se valorizzato, verifica l'idoneità all'anticipazione per acquisto prima casa)
 * @param pensionFundBalance montante accumulato nel fondo pensione, per stimare l'anticipazione
 *                         potenziale (opzionale, usato solo se {@code pensionFundYears} è valorizzato)
 * @param homeSale         dati della casa esistente da vendere, per stimare il capitale disponibile
 *                         dalla vendita (opzionale; se null, questa fonte non viene considerata)
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

        Double monthlyNetIncome,

        String purchaseType,

        @PositiveOrZero(message = "notaryCosts deve essere >= 0")
        Double notaryCosts,

        @PositiveOrZero(message = "originationFees deve essere >= 0")
        Double originationFees,

        @PositiveOrZero(message = "appraisalFees deve essere >= 0")
        Double appraisalFees,

        @PositiveOrZero(message = "agencyFeePct deve essere >= 0")
        Double agencyFeePct,

        @PositiveOrZero(message = "agencyFeeAmount deve essere >= 0")
        Double agencyFeeAmount,

        @PositiveOrZero(message = "registrationTax deve essere >= 0")
        Double registrationTax,

        @PositiveOrZero(message = "liquidSavings deve essere >= 0")
        Double liquidSavings,

        @PositiveOrZero(message = "pensionFundYears deve essere >= 0")
        Integer pensionFundYears,

        @PositiveOrZero(message = "pensionFundBalance deve essere >= 0")
        Double pensionFundBalance,

        @Valid
        HomeSaleRequest homeSale
) {}
