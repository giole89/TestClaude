package com.finai.dto.finance;

import com.finai.domain.entity.FixedExpense;

import java.time.Instant;

/** DTO di risposta per una spesa fissa. */
public record FixedExpenseDto(
        String  id,
        String  name,
        String  category,
        Double  amount,
        boolean active,
        Double  interestRatePct,
        Instant createdAt
) {
    public static FixedExpenseDto from(FixedExpense e) {
        return new FixedExpenseDto(e.getId(), e.getName(), e.getCategory(), e.getAmount().doubleValue(), e.isActive(),
                e.getInterestRatePct() != null ? e.getInterestRatePct().doubleValue() : null, e.getCreatedAt());
    }
}
