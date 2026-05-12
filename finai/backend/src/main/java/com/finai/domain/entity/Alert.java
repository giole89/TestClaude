package com.finai.domain.entity;

import com.finai.domain.enums.AlertType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Alert sul prezzo di un titolo.
 *
 * <p>Un alert è <em>attivo</em> finché {@code firedAt} è null.
 * Quando scatta (via {@code POST /api/alerts/:id/fire}), vengono impostati
 * {@code firedAt} e {@code firedPrice}, spostando l'alert in "history".</p>
 */
@Entity
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor
public class Alert {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(nullable = false, length = 20)
    private String ticker;

    /**
     * Tipo di alert. Mappato come stringa lowercase in DB per leggibilità
     * (es. "above", "below", "change_up", "change_down").
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertType type;

    /** Valore soglia (prezzo assoluto o percentuale di variazione). */
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal value;

    /** Timestamp di scatto. Null = alert ancora attivo. */
    @Column(name = "fired_at")
    private Instant firedAt;

    /** Prezzo al momento dello scatto. */
    @Column(name = "fired_price", precision = 18, scale = 4)
    private BigDecimal firedPrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    /** @return true se l'alert non è ancora scattato */
    @Transient
    public boolean isActive() {
        return firedAt == null;
    }
}
