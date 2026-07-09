package com.finai.dto.finance.mortgage;

/**
 * Quanto mutuo si potrebbe ragionevolmente richiedere, calcolato al contrario a partire dal reddito
 * dichiarato/stimato, dagli altri debiti già in essere, dal tasso e dalla durata indicati, e dal
 * vincolo di LTV che le banche applicano ai mutui fondiari.
 *
 * @param maxLoanComfortable     mutuo massimo tale per cui la rata (+ altri debiti) resta entro la soglia "Sostenibile" (30% del reddito)
 * @param maxLoanAtLimit         mutuo massimo tale per cui la rata (+ altri debiti) resta entro la soglia limite (35% del reddito)
 * @param maxLoanByLtv           mutuo massimo consentito dal solo vincolo di LTV (80% del valore dell'immobile)
 * @param recommendedMaxLoan     mutuo massimo consigliato: il più basso tra {@code maxLoanAtLimit} e {@code maxLoanByLtv}
 * @param bindingConstraint      "REDDITO" o "LTV": quale dei due vincoli determina il mutuo massimo consigliato
 * @param requestedLoanAmount    l'importo di mutuo effettivamente simulato in questa richiesta, per il confronto
 * @param requestedLoanNote      confronto testuale tra l'importo richiesto e il massimo consigliato
 * @param minLoanNeededGivenCapital mutuo minimo necessario per coprire l'acquisto dato il capitale disponibile dichiarato
 *                               (liquidità + eventuale vendita); 0 se il capitale copre già tutto
 * @param equityRatioAtRecommendedPct quota di capitale proprio sul valore dell'immobile se si richiedesse il mutuo massimo consigliato, in percentuale
 * @param note                   sintesi testuale del ragionamento e dei vincoli applicati
 */
public record MaxLoanAdviceDto(
        Double maxLoanComfortable,
        Double maxLoanAtLimit,
        Double maxLoanByLtv,
        Double recommendedMaxLoan,
        String bindingConstraint,
        Double requestedLoanAmount,
        String requestedLoanNote,
        Double minLoanNeededGivenCapital,
        Double equityRatioAtRecommendedPct,
        String note
) {}
