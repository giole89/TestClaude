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
 * @param saleAgencyFees        spese di agenzia per la vendita totali, IVA inclusa (dichiarate o stimate)
 * @param saleAgencyFeesEstimated true se stimate
 * @param saleAgencyFeesBase    quota di commissione al netto dell'IVA
 * @param saleAgencyFeesIva     quota di IVA sulla commissione (22%, aggiunta automaticamente solo se indicata/stimata
 *                              in percentuale; 0 se dichiarato un importo finale in euro)
 * @param saleAgencyFeeMode     "PERCENTAGE" se calcolata da una percentuale (con IVA aggiunta automaticamente),
 *                              "AMOUNT" se da un importo finale in euro dichiarato
 * @param residualMortgageBalance mutuo/finanziamento residuo da estinguere sulla casa venduta
 * @param netProceeds           capitale netto disponibile dopo spese, mutuo residuo ed eventuale imposta;
 *                              può essere negativo se i costi superano il valore di vendita
 * @param monthsUntilSale       mesi dichiarati fino al completamento della vendita, se indicati
 * @param timingNote            nota sul confronto tra i tempi previsti e i tempi medi di vendita in Italia; null se non applicabile
 * @param summary               riepilogo testuale del risultato
 * @param mustFullyFundPurchase true se questa vendita è stata dichiarata come unica fonte di capitale, che deve
 *                              coprire da sola capitale proprio e spese accessorie del nuovo acquisto
 * @param coversFullPurchase    true se il capitale netto di questa vendita, da solo, copre l'intero costo non
 *                              finanziato dal mutuo (capitale proprio + spese accessorie)
 * @param fundingGapOrSurplus   {@code netProceeds - totalOutOfPocketCost}: positivo se la vendita basta e avanza,
 *                              negativo se manca capitale anche considerando solo questa fonte
 * @param fullFundingNote       avviso con priorità e alternative concrete se {@code mustFullyFundPurchase} è vero
 *                              e il capitale netto non basta da solo; null altrimenti
 */
public record HomeSaleAdviceDto(
        Double capitalGain,
        boolean capitalGainsTaxable,
        Double capitalGainsTax,
        String capitalGainsNote,
        Double saleAgencyFees,
        boolean saleAgencyFeesEstimated,
        Double saleAgencyFeesBase,
        Double saleAgencyFeesIva,
        String saleAgencyFeeMode,
        Double residualMortgageBalance,
        Double netProceeds,
        Integer monthsUntilSale,
        String timingNote,
        String summary,
        boolean mustFullyFundPurchase,
        boolean coversFullPurchase,
        Double fundingGapOrSurplus,
        String fullFundingNote
) {}
