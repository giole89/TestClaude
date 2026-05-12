package com.finai.dto.alert;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Corpo della richiesta {@code POST /api/alerts/:id/fire}. */
public record FireAlertRequest(
        @NotNull @Positive Double price
) {}
