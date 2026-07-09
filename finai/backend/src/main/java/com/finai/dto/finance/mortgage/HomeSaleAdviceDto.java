package com.finai.dto.finance.mortgage;

/**
 * Stima del capitale disponibile dalla vendita di una casa esistente, al netto di spese di
 * agenzia, mutuo/finanziamento residuo da estinguere ed eventuale imposta sulla plusvalenza.
 *
 * @param capitalGain           plusvalenza lorda (valore di vendita − prezzo di acquisto, 0 se non c'è guadagno)
 * @param capitalGainsTaxable   true se la plusvalenza è tassabile (immobile posseduto da meno di 5 anni e non
 *                              abitazione principale per la maggior parte del periodo di possesso)
 * @param capitalGainsTax       imposta sostitutiva stimata (26% della plusvalenza, se tassabile)
 * @param capitalGainsNote      spiegazione della regola applicata (esenzione o tassazione)
 * @param saleAgencyFees        spese di agenzia per la vendita (dichiarate o stimate)
 * @param saleAgencyFeesEstimated true se stimate
 * @param residualMortgageBalance mutuo/finanziamento residuo da estinguere sulla casa venduta
 * @param netProceeds           capitale netto disponibile dopo spese, mutuo residuo ed eventuale imposta;
 *                              può essere negativo se i costi superano il valore di vendita
 * @param monthsUntilSale       mesi dichiarati fino al completamento della vendita, se indicati
 * @param timingNote            nota sul confronto tra i tempi previsti e i tempi medi di vendita in Italia; null se non applicabile
 * @param summary               riepilogo testuale del risultato
 */
public record HomeSaleAdviceDto(
        Double capitalGain,
        boolean capitalGainsTaxable,
        Double capitalGainsTax,
        String capitalGainsNote,
        Double saleAgencyFees,
        boolean saleAgencyFeesEstimated,
        Double residualMortgageBalance,
        Double netProceeds,
        Integer monthsUntilSale,
        String timingNote,
        String summary
) {}
