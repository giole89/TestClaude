package com.finai.dto.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Movimento grezzo estratto da un file di estratto conto (PDF o Excel),
 * prima della categorizzazione.
 */
public record RawTransaction(LocalDate date, String description, BigDecimal amount) {}
