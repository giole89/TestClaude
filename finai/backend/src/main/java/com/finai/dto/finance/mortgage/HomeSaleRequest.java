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
 * @param saleAgencyFees  spese di agenzia per la vendita; se null, stima indicativa (~3% + IVA del valore di vendita)
 * @param monthsUntilSale  tra quanti mesi prevedi di completare la vendita (opzionale, per una nota sui tempi)
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

        @PositiveOrZero(message = "saleAgencyFees deve essere >= 0")
        Double saleAgencyFees,

        @PositiveOrZero(message = "monthsUntilSale deve essere >= 0")
        Integer monthsUntilSale
) {}
