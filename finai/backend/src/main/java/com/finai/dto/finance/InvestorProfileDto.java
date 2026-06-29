package com.finai.dto.finance;

import com.finai.domain.entity.InvestorProfile;

/** DTO di risposta per il profilo investitore (questionario). */
public record InvestorProfileDto(String goal, String goalNote, String horizon, Double liquidSavings, boolean completed) {

    public static InvestorProfileDto from(InvestorProfile e) {
        return new InvestorProfileDto(e.getGoal(), e.getGoalNote(), e.getHorizon(),
                e.getLiquidSavings() != null ? e.getLiquidSavings().doubleValue() : null,
                e.getGoal() != null && e.getHorizon() != null);
    }
}
