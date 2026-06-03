package com.finai.dto.news;

/** Notizia finanziaria da Yahoo Finance. */
public record NewsItemDto(
        String title,
        String url,
        String publisher,
        /** Timestamp pubblicazione (ms). */
        Long publishedAt,
        String summary,
        String ticker
) {}
