package com.finai.dto.finance.mortgage;

import java.util.List;

/**
 * Esito della simulazione di un finanziamento/prestito personale: rata, costo totale e
 * rapporto rata/reddito comprensivo dei debiti già in essere tra le spese fisse.
 *
 * @param otherActiveDebtPayments somma delle rate mensili di altri debiti/finanziamenti già tra le spese fisse
 * @param paymentToIncomeRatioPct rapporto rata del solo finanziamento / reddito netto, in percentuale
 * @param combinedPaymentToIncomeRatioPct rapporto (rata + altri debiti già in essere) / reddito netto, in percentuale
 */
public record LoanSimulationDto(
        Double monthlyPayment,
        Double totalPaid,
        Double totalInterest,
        Double monthlyNetIncome,
        boolean incomeEstimated,
        Double otherActiveDebtPayments,
        Double paymentToIncomeRatioPct,
        Double combinedPaymentToIncomeRatioPct,
        String affordabilityLabel,
        String affordabilityWarning,
        List<AmortizationYearDto> schedule
) {}
