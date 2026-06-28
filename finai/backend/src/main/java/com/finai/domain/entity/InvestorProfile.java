package com.finai.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Profilo investitore raccolto tramite questionario: a cosa serviranno i
 * soldi investiti e tra quanto tempo l'utente prevede di averne bisogno.
 *
 * <p>Esiste sempre una sola riga con {@code id = "default"}, come per
 * {@link SimWallet}: l'app non ha autenticazione multi-utente.</p>
 */
@Entity
@Table(name = "investor_profile")
@Getter
@Setter
@NoArgsConstructor
public class InvestorProfile {

    @Id
    @Column(length = 20, nullable = false)
    private String id = "default";

    /** Obiettivo dell'investimento: EMERGENCY | MAJOR_PURCHASE | RETIREMENT | GROWTH | OTHER. */
    @Column(length = 50)
    private String goal;

    @Column(name = "goal_note", length = 255)
    private String goalNote;

    /** Orizzonte temporale: UNDER_1Y | Y1_3 | Y3_5 | Y5_10 | OVER_10Y. */
    @Column(length = 30)
    private String horizon;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
