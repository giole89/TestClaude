package com.finai.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Singolo messaggio nella conversazione AI. */
public record ChatMessage(
        @NotBlank
        @Pattern(regexp = "user|assistant", message = "role deve essere 'user' o 'assistant'")
        String role,

        @NotBlank
        String content
) {}
