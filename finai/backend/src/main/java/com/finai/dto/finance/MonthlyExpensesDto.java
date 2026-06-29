package com.finai.dto.finance;

import java.util.List;

/** Spese variabili effettivamente sostenute nel mese corrente, suddivise per categoria (per il grafico a torta). */
public record MonthlyExpensesDto(String periodLabel, Double total, List<CategoryAmountDto> byCategory) {}
