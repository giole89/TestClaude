package com.finai.dto.finance.mortgage;

/**
 * Rata, interessi totali e sostenibilità dello stesso importo di mutuo simulato con una durata
 * diversa da quella scelta, per confrontare il classico compromesso rata-più-bassa/interessi-più-alti
 * su un ventaglio di durate tipiche (10/15/20/25/30 anni).
 */
public record DurationOptionDto(
        int years,
        Double monthlyPayment,
        Double totalInterest,
        Double combinedPaymentToIncomeRatioPct,
        String affordabilityLabel,
        boolean isSelected
) {}
