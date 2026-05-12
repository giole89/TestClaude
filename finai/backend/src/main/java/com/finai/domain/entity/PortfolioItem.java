package com.finai.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Posizione nel portafoglio personale dell'utente.
 *
 * <p>{@code loadPrice} rappresenta il prezzo medio di carico; {@code currentPrice}
 * viene aggiornato periodicamente tramite l'endpoint {@code POST /api/portfolio/refresh}.</p>
 */
@Entity
@Table(name = "portfolio_items")
@Getter
@Setter
@NoArgsConstructor
public class PortfolioItem {

    /** ID generato lato client (UUID v4) per compatibilità con il frontend. */
    @Id
    @Column(length = 36, nullable = false)
    private String id;

    /** Simbolo Yahoo Finance (es. AAPL, ISP.MI). Sempre uppercase. */
    @Column(nullable = false, length = 20)
    private String ticker;

    /** Nome completo dello strumento (es. "Apple Inc."). */
    @Column(nullable = false)
    private String name;

    /** Quantità di unità detenute. */
    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal qty;

    /** Prezzo medio di carico. */
    @Column(name = "load_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal loadPrice;

    /** Ultimo prezzo aggiornato via Yahoo Finance. Null fino al primo refresh. */
    @Column(name = "current_price", precision = 18, scale = 4)
    private BigDecimal currentPrice;

    /** Valuta del titolo (es. USD, EUR). */
    @Column(nullable = false, length = 10)
    private String currency = "USD";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
