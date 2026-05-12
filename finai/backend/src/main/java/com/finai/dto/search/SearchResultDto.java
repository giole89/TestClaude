package com.finai.dto.search;

/** Risultato dell'autocomplete ticker ({@code GET /api/search?q=}). */
public record SearchResultDto(
        String ticker,
        String name,
        String exchange,
        /** Tipo strumento: EQUITY | ETF | MUTUALFUND | INDEX | FUTURE | CURRENCY */
        String type
) {}
