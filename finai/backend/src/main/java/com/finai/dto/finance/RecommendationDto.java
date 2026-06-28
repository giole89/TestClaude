package com.finai.dto.finance;

import java.util.List;

/** Esito del motore di consiglio investimenti basato sul questionario. */
public record RecommendationDto(
        String profileLabel,
        AllocationDto allocation,
        String summary,
        List<String> suggestedInstruments,
        String goal,
        String horizon
) {}
