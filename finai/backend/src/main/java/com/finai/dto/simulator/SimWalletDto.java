package com.finai.dto.simulator;

import com.finai.domain.entity.SimWallet;

import java.time.Instant;

/** DTO di risposta per lo stato grezzo del wallet virtuale (senza valorizzazione posizioni). */
public record SimWalletDto(
        Double  cashBalance,
        Double  startingBalance,
        Instant resetAt
) {
    public static SimWalletDto from(SimWallet w) {
        return new SimWalletDto(
                w.getCashBalance().doubleValue(),
                w.getStartingBalance().doubleValue(),
                w.getResetAt()
        );
    }
}
