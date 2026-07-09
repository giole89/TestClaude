package com.finai.dto.finance.mortgage;

/** Stima del reddito netto mensile calcolata dal budget, usata per precompilare il calcolatore mutuo/finanziamento. */
public record IncomeEstimateDto(
        Double estimatedMonthlyIncome,
        boolean hasEnoughData
) {}
