package com.finai.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Elemento nella watchlist personale dell'utente.
 *
 * <p>Permette di monitorare ticker con un target price opzionale e note.</p>
 */
@Entity
@Table(name = "watchlist_items")
@Getter
@Setter
@NoArgsConstructor
public class WatchlistItem {

    @Id
    @Column(length = 50, nullable = false)
    private String id;

    @Column(nullable = false, length = 20, unique = true)
    private String ticker;

    @Column(length = 255)
    private String name;

    @Column(name = "target_price", precision = 12, scale = 4)
    private BigDecimal targetPrice;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false)
    private Long createdAt = System.currentTimeMillis();

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = System.currentTimeMillis();
    }
}
