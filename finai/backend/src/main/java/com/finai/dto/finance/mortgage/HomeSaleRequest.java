package com.finai.dto.finance.mortgage;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Dati della casa esistente da vendere, per stimare il capitale disponibile per il nuovo acquisto.
 *
 * @param saleValue       stima del valore di vendita
 * @param purchasePrice   prezzo a cui è stata acquistata (per calcolare l'eventuale plusvalenza)
 * @param yearsOwned      anni di possesso dell'immobile (rilevante per la tassazione della plusvalenza)
 * @param mainResidence   true se è stata abitazione principale per la maggior parte del periodo di possesso
 *                        (esenzione dalla tassazione della plusvalenza indipendentemente dagli anni di possesso)
 * @param residualMortgageBalance mutuo/finanziamento residuo da estinguere sulla casa venduta, se presente
 * @param saleAgencyFeePct percentuale di commissione dell'agenzia per la vendita (esclusa IVA), se preferisci
 *                        indicarla in percentuale anziché in euro; l'IVA al 22% viene aggiunta automaticamente.
 *                        Ha priorità su {@code saleAgencyFeeAmount} se entrambi sono valorizzati
 * @param saleAgencyFeeAmount spese di agenzia per la vendita in euro, importo finale già comprensivo di IVA;
 *                        usato solo se {@code saleAgencyFeePct} è null. Se nessuno dei due è valorizzato,
 *                        stima indicativa (~3% + IVA del valore di vendita)
 * @param monthsUntilSale  tra quanti mesi prevedi di completare la vendita (opzionale, per una nota sui tempi)
 * @param mustFullyFundPurchase true se il capitale netto di questa vendita è l'unica fonte su cui puoi contare e
 *                        deve coprire da solo capitale proprio (anticipo) e tutte le spese accessorie del nuovo
 *                        acquisto, senza altra liquidità di riserva: se il capitale netto non basta, FINAI lo
 *                        segnala con priorità e propone alternative concrete invece di dare per scontato un
 *                        margine di sicurezza che non hai
 */
public record HomeSaleRequest(
        @NotNull(message = "saleValue obbligatorio")
        @Positive(message = "saleValue deve essere positivo")
        Double saleValue,

        @NotNull(message = "purchasePrice obbligatorio")
        @Positive(message = "purchasePrice deve essere positivo")
        Double purchasePrice,

        @NotNull(message = "yearsOwned obbligatorio")
        @PositiveOrZero(message = "yearsOwned deve essere >= 0")
        Integer yearsOwned,

        Boolean mainResidence,

        @PositiveOrZero(message = "residualMortgageBalance deve essere >= 0")
        Double residualMortgageBalance,

        @PositiveOrZero(message = "saleAgencyFeePct deve essere >= 0")
        Double saleAgencyFeePct,

        @PositiveOrZero(message = "saleAgencyFeeAmount deve essere >= 0")
        Double saleAgencyFeeAmount,

        @PositiveOrZero(message = "monthsUntilSale deve essere >= 0")
        Integer monthsUntilSale,

        Boolean mustFullyFundPurchase
) {}
