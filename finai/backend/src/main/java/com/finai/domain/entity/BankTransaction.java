package com.finai.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Riga di movimento importata da un estratto conto (PDF o Excel).
 *
 * <p>{@code amount} è negativo per le uscite e positivo per le entrate,
 * coerentemente con il formato tipico degli estratti conto bancari.</p>
 */
@Entity
@Table(name = "bank_transactions")
@Getter
@Setter
@NoArgsConstructor
public class BankTransaction {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "tx_date", nullable = false)
    private LocalDate txDate;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 50)
    private String category;

    /** {@code INCOME} oppure {@code VARIABLE_EXPENSE}. */
    @Column(nullable = false, length = 20)
    private String type;

    @Column(name = "source_file", length = 255)
    private String sourceFile;

    @Column(name = "imported_at", nullable = false, updatable = false)
    private Instant importedAt = Instant.now();
}
