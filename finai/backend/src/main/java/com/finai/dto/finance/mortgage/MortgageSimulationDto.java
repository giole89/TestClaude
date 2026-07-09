package com.finai.dto.finance.mortgage;

import java.util.List;

/**
 * Esito della simulazione di un mutuo: rata, costo totale, indicatori di sostenibilità
 * (LTV, rapporto rata/reddito comprensivo dei debiti già in essere, stress test tassi), il
 * costo complessivo dell'acquisto non coperto dal mutuo (capitale proprio + spese accessorie)
 * e a quali fonti attingere per coprirlo, piano di ammortamento annuale incluso.
 *
 * @param loanToValuePct         rapporto importo mutuo / valore immobile (LTV), in percentuale
 * @param ltvWarning             avviso se l'LTV supera la soglia tipica dei mutui fondiari italiani (80%); null altrimenti
 * @param monthlyNetIncome       reddito netto mensile usato per il calcolo (dichiarato o stimato)
 * @param incomeEstimated        true se il reddito non è stato dichiarato ed è stato stimato dal budget
 * @param otherActiveDebtPayments somma delle rate mensili di altri debiti/finanziamenti già tra le spese fisse
 * @param paymentToIncomeRatioPct rapporto rata del solo mutuo / reddito netto, in percentuale
 * @param combinedPaymentToIncomeRatioPct rapporto (rata mutuo + altri debiti già in essere) / reddito netto, in percentuale
 * @param affordabilityLabel     giudizio sintetico di sostenibilità ("Sostenibile" / "Al limite" / "Rischioso")
 * @param affordabilityWarning   avviso testuale se il rapporto combinato supera la soglia consigliata; null altrimenti
 * @param stressTestRatePct      tasso simulato in caso di rialzo (interestRatePct + stress)
 * @param stressTestMonthlyPayment rata mensile ricalcolata al tasso di stress
 * @param stressTestCombinedRatioPct rapporto rata+altri debiti / reddito al tasso di stress, in percentuale
 * @param stressTestWarning      avviso se lo stress test porta il rapporto oltre la soglia di sostenibilità; null altrimenti
 * @param downPayment            capitale proprio necessario: valore immobile meno importo mutuo
 * @param notaryCosts            costi di notaio (dichiarati o stimati)
 * @param notaryCostsEstimated   true se stimati (non dichiarati dall'utente)
 * @param originationFees        spese di istruttoria bancaria (dichiarate o stimate)
 * @param originationFeesEstimated true se stimate
 * @param appraisalFees          spese di perizia dell'immobile (dichiarate o stimate)
 * @param appraisalFeesEstimated true se stimate
 * @param agencyFees             spese di agenzia immobiliare totali, IVA inclusa (dichiarate o stimate)
 * @param agencyFeesEstimated    true se stimate
 * @param agencyFeesBase         quota di commissione dell'agenzia al netto dell'IVA
 * @param agencyFeesIva          quota di IVA sulla commissione (22%, aggiunta automaticamente solo quando la
 *                               commissione è indicata/stimata in percentuale; 0 se dichiarato un importo finale in euro)
 * @param agencyFeeMode          "PERCENTAGE" se la commissione è stata calcolata da una percentuale (con IVA aggiunta
 *                               automaticamente), "AMOUNT" se da un importo finale in euro dichiarato
 * @param registrationTax        imposta di registro/IVA sull'acquisto (dichiarata o stimata)
 * @param registrationTaxEstimated true se stimata
 * @param registrationTaxNote    nota sui limiti della stima dell'imposta (approssimata sul prezzo, non sul valore catastale)
 * @param totalAncillaryCosts    somma di notaio + istruttoria + perizia + agenzia + imposta di registro/IVA
 * @param totalOutOfPocketCost   tutto ciò che non rientra nel mutuo: capitale proprio + spese accessorie totali
 * @param availableLiquidSavings liquidità disponibile per coprire il costo non finanziato (dichiarata o dal profilo investitore)
 * @param liquidSavingsSource    "DECLARED" se dichiarata in questa simulazione, "PROFILE" se presa dal questionario investitore, "NONE" se non disponibile
 * @param homeSale               stima del capitale disponibile dalla vendita di una casa esistente; null se non dichiarata
 * @param totalAvailableCapital  liquidità disponibile più l'eventuale capitale netto dalla vendita di una casa esistente (se positivo)
 * @param shortfall              fabbisogno residuo non coperto dal capitale disponibile complessivo (0 se basta)
 * @param pensionFund            idoneità e stima dell'anticipazione del fondo pensione; null se non dichiarati anni di iscrizione
 * @param budgetAdvice           elenco ordinato di fonti a cui attingere per coprire il fabbisogno residuo
 */
public record MortgageSimulationDto(
        Double monthlyPayment,
        Double totalPaid,
        Double totalInterest,
        Double loanToValuePct,
        String ltvWarning,
        Double monthlyNetIncome,
        boolean incomeEstimated,
        Double otherActiveDebtPayments,
        Double paymentToIncomeRatioPct,
        Double combinedPaymentToIncomeRatioPct,
        String affordabilityLabel,
        String affordabilityWarning,
        Double stressTestRatePct,
        Double stressTestMonthlyPayment,
        Double stressTestCombinedRatioPct,
        String stressTestWarning,
        Double downPayment,
        Double notaryCosts,
        boolean notaryCostsEstimated,
        Double originationFees,
        boolean originationFeesEstimated,
        Double appraisalFees,
        boolean appraisalFeesEstimated,
        Double agencyFees,
        boolean agencyFeesEstimated,
        Double agencyFeesBase,
        Double agencyFeesIva,
        String agencyFeeMode,
        Double registrationTax,
        boolean registrationTaxEstimated,
        String registrationTaxNote,
        Double totalAncillaryCosts,
        Double totalOutOfPocketCost,
        Double availableLiquidSavings,
        String liquidSavingsSource,
        HomeSaleAdviceDto homeSale,
        Double totalAvailableCapital,
        Double shortfall,
        PensionFundAdviceDto pensionFund,
        List<BudgetAdviceDto> budgetAdvice,
        List<AmortizationYearDto> schedule
) {}
