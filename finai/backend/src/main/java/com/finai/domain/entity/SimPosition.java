package com.finai.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Posizione aperta nel portafoglio simulato.
 *
 * <p>{@code avgPrice} è il prezzo medio di carico ponderato: aggiornato ad
 * ogni acquisto successivo dello stesso ticker, invariato in vendita.</p>
 */
@Entity
@Table(name = "sim_positions")
@Getter
@Setter
@NoArgsConstructor
public class SimPosition {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(nullable = false, length = 20, unique = true)
    private String ticker;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal qty;

    @Column(name = "avg_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal avgPrice;

    @Column(nullable = false, length = 10)
    private String currency = "USD";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
