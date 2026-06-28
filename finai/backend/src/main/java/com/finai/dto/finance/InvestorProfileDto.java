package com.finai.dto.finance;

import com.finai.domain.entity.InvestorProfile;

/** DTO di risposta per il profilo investitore (questionario). */
public record InvestorProfileDto(String goal, String goalNote, String horizon, boolean completed) {

    public static InvestorProfileDto from(InvestorProfile e) {
        return new InvestorProfileDto(e.getGoal(), e.getGoalNote(), e.getHorizon(),
                e.getGoal() != null && e.getHorizon() != null);
    }
}
