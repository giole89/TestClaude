package com.finai.dto.finance.mortgage;

/** Riepilogo annuale del piano di ammortamento: quota capitale/interessi pagata nell'anno e debito residuo a fine anno. */
public record AmortizationYearDto(
        int year,
        Double principalPaid,
        Double interestPaid,
        Double remainingBalance
) {}
