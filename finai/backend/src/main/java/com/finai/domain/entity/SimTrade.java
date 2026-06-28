package com.finai.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Riga di storico append-only per ogni operazione BUY/SELL simulata.
 *
 * <p>{@code realizedPnl} è popolato solo per le vendite: differenza tra
 * prezzo di vendita e prezzo medio di carico, moltiplicata per la quantità.</p>
 */
@Entity
@Table(name = "sim_trades")
@Getter
@Setter
@NoArgsConstructor
public class SimTrade {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(nullable = false, length = 20)
    private String ticker;

    @Column(nullable = false)
    private String name;

    /** "BUY" oppure "SELL". */
    @Column(nullable = false, length = 10)
    private String side;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal qty;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal price;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    @Column(name = "realized_pnl", precision = 18, scale = 4)
    private BigDecimal realizedPnl;

    @Column(nullable = false, length = 10)
    private String currency = "USD";

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt = Instant.now();

    @PrePersist
    void onCreate() {
        if (executedAt == null) executedAt = Instant.now();
    }
}
