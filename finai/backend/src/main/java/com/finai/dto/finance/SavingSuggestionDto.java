package com.finai.dto.finance;

/**
 * Suggerimento concreto di risparmio generato analizzando i movimenti reali
 * dell'utente (non un consiglio generico): {@code potentialMonthlySaving} è
 * una stima in euro/mese di quanto si libererebbe seguendo il suggerimento.
 */
public record SavingSuggestionDto(
        String type,
        String category,
        String severity,
        String title,
        String message,
        Double potentialMonthlySaving
) {}
