package com.finai.dto.earnings;

/** Prossima data di earnings per un ticker. */
public record EarningsDto(
        String ticker,
        String companyName,
        /** Unix timestamp (ms) inizio finestra earnings. Null se non disponibile. */
        Long earningsStart,
        /** Unix timestamp (ms) fine finestra earnings. */
        Long earningsEnd,
        /** Data formattata (yyyy-MM-dd). */
        String earningsDate,
        /** EPS stimato per il prossimo trimestre. */
        Double epsForward,
        /** EPS trailing twelve months. */
        Double epsTrailing,
        /** P/E forward. */
        Double forwardPE,
        /** Trimestre di riferimento (es. "Q2 2025"). */
        String quarter
) {}
