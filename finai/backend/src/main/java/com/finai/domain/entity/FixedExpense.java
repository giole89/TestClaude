package com.finai.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Costo fisso mensile inserito manualmente dall'utente (affitto, mutuo,
 * utenze, abbonamenti, ecc.), usato dal motore di budget per il mese
 * successivo.
 */
@Entity
@Table(name = "fixed_expenses")
@Getter
@Setter
@NoArgsConstructor
public class FixedExpense {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private boolean active = true;

    /** Tasso di interesse annuo (%) se questa spesa fissa è il pagamento di un debito/finanziamento. Null se non è un debito. */
    @Column(name = "interest_rate_pct", precision = 5, scale = 2)
    private BigDecimal interestRatePct;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
