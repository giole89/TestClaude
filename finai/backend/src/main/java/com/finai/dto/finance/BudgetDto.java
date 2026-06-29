package com.finai.dto.finance;

import java.util.List;

/**
 * Budget previsionale per il mese successivo: entrate stimate, costi fissi
 * (inseriti dall'utente), costi variabili stimati (dalla media storica degli
 * estratti conto importati) e quota di risparmio investibile residua.
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
) {}
