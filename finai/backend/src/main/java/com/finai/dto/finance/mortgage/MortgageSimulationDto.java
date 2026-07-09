package com.finai.dto.finance.mortgage;

import java.util.List;

/**
 * Esito della simulazione di un mutuo: rata, costo totale, indicatori di sostenibilità
 * (LTV, rapporto rata/reddito comprensivo dei debiti già in essere, stress test tassi) e
 * piano di ammortamento annuale.
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
 * @param estimatedAncillaryCosts stima indicativa di spese accessorie (notaio, imposte, perizia, istruttoria)
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
        Double estimatedAncillaryCosts,
        List<AmortizationYearDto> schedule
) {}
