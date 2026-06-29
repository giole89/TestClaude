package com.finai.dto.finance;

import java.util.List;

/** Esito dell'analisi dell'estratto conto: suggerimenti ordinati per impatto decrescente. */
public record SpendingInsightsDto(
        List<SavingSuggestionDto> suggestions,
        Double totalPotentialMonthlySaving
) {}
