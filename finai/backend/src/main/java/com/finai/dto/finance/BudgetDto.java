package com.finai.dto.finance;

import java.util.List;

/**
 * Budget previsionale per il mese successivo: entrate stimate, costi fissi
 * (inseriti dall'utente), costi variabili stimati (dalla media storica degli
 * estratti conto importati) e saldo residuo.
 *
 * <p>{@code projectedSavings} è il saldo reale (entrate - costi fissi - costi
 * variabili stimati) e può essere negativo: in tal caso il mese successivo è
 * previsto in perdita. {@code investableAmount} è invece la quota
 * effettivamente investibile, sempre &gt;= 0.</p>
 */
public record BudgetDto(
        String periodLabel,
        Double estimatedIncome,
        Double fixedCosts,
        Double variableCostsEstimate,
        List<CategoryAmountDto> variableByCategory,
        List<CategoryAmountDto> incomeByCategory,
        Double projectedSavings,
        Double investableAmount,
        int monthsOfHistory,
        boolean hasEnoughData,
        boolean basedOnCurrentMonthOnly
) {
    public boolean isDeficit() {
        return projectedSavings != null && projectedSavings < 0;
    }
}
