package com.finai.dto.finance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Risposte al questionario sull'obiettivo di investimento.
 *
 * @param goal     EMERGENCY | MAJOR_PURCHASE | RETIREMENT | GROWTH | OTHER
 * @param goalNote nota libera, usata principalmente quando {@code goal == OTHER}
 * @param horizon  UNDER_1Y | Y1_3 | Y3_5 | Y5_10 | OVER_10Y
 */
public record QuestionnaireRequest(
        @NotBlank(message = "goal obbligatorio")
        String goal,

        @Size(max = 255)
        String goalNote,

        @NotBlank(message = "horizon obbligatorio")
        String horizon
) {}
