package com.finai.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Elemento della watchlist IPO personale.
 *
 * <p>Tiene traccia di IPO da monitorare con data prevista, prezzo e
 * periodo di lock-up. {@code ipoDate} viene aggiornato con PATCH dopo
 * l'effettiva quotazione, abilitando il calcolo dei giorni di lock-up rimanenti.</p>
 */
@Entity
@Table(name = "ipo_watchlist")
@Getter
@Setter
@NoArgsConstructor
public class IpoWatchlistItem {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    /** Ticker Yahoo Finance se già noto (può essere null per IPO pre-quotazione). */
    @Column(length = 20)
    private String ticker;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    /** Data prevista di quotazione. */
    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @Column(length = 50)
    private String exchange;

    @Column(length = 100)
    private String sector;

    /**
     * Durata del lock-up in giorni (default 180).
     * Il lock-up impedisce ai dipendenti/insider di vendere azioni dopo la quotazione.
     */
    @Column(name = "lockup_days", nullable = false)
    private int lockupDays = 180;

    /** Prezzo IPO effettivo o stimato in USD. */
    @Column(name = "ipo_price", precision = 18, scale = 4)
    private BigDecimal ipoPrice;

    /** Data di effettiva quotazione (aggiornata post-IPO). */
    @Column(name = "ipo_date")
    private LocalDate ipoDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    /**
     * Calcola i giorni rimanenti al termine del lock-up.
     *
     * @return giorni rimanenti (negativo = lock-up scaduto), oppure null se ipoDate non impostata
     */
    @Transient
    public Long lockupRemainingDays() {
        if (ipoDate == null) return null;
        LocalDate lockupEnd = ipoDate.plusDays(lockupDays);
        return (long) LocalDate.now().until(lockupEnd).getDays();
    }
}
