package com.finai.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Wallet virtuale dell'ambiente di simulazione (paper trading).
 *
 * <p>Esiste sempre una sola riga con {@code id = "default"}: l'app non ha
 * autenticazione multi-utente, quindi la simulazione è singola per istanza.</p>
 */
@Entity
@Table(name = "sim_wallet")
@Getter
@Setter
@NoArgsConstructor
public class SimWallet {

    @Id
    @Column(length = 20, nullable = false)
    private String id = "default";

    @Column(name = "cash_balance", nullable = false, precision = 18, scale = 4)
    private BigDecimal cashBalance;

    @Column(name = "starting_balance", nullable = false, precision = 18, scale = 4)
    private BigDecimal startingBalance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "reset_at", nullable = false)
    private Instant resetAt = Instant.now();
}
