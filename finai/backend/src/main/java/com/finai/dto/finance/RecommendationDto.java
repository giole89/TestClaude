package com.finai.dto.finance;

import java.util.List;

/**
 * Esito del motore di consiglio investimenti basato sul questionario.
 *
 * @param emergencyFundWarning   non null se la liquidità dichiarata non copre 3-6 mesi di spese:
 *                               prima di investire conviene completare il fondo di emergenza
 * @param highInterestDebtWarning non null se tra le spese fisse c'è un debito ad alto interesse:
 *                                estinguerlo ha priorità rispetto a investire
 * @param pacNote                suggerimento di investire la quota investibile (ricorrente, mensile)
 *                                tramite un piano di accumulo (PAC) invece che in un'unica soluzione
 */
public record RecommendationDto(
        String profileLabel,
        AllocationDto allocation,
        String summary,
        List<String> suggestedInstruments,
        String goal,
        String horizon,
        MarketSnapshotDto marketSnapshot,
        List<PortfolioLineDto> samplePortfolio,
        String emergencyFundWarning,
        String highInterestDebtWarning,
        String pacNote
) {}
