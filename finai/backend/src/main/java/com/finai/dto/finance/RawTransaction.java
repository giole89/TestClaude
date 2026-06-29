package com.finai.dto.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Movimento grezzo estratto da un file di estratto conto (PDF o Excel),
 * prima della categorizzazione.
 *
 * <p>{@code sourceCategory} è la categoria eventualmente già assegnata dalla banca stessa
 * (es. colonna "CATEGORIA" di alcuni estratti conto): se presente, è più precisa del
 * riconoscimento per parole chiave nella descrizione e va preferita. {@code null} quando il
 * formato del file non fornisce questa informazione.</p>
 */
public record RawTransaction(LocalDate date, String description, BigDecimal amount, String sourceCategory) {

    public RawTransaction(LocalDate date, String description, BigDecimal amount) {
        this(date, description, amount, null);
    }
}
