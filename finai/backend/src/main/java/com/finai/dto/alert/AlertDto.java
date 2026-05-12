package com.finai.dto.alert;

import com.finai.domain.entity.Alert;

import java.time.Instant;

/** DTO di risposta per un alert. */
public record AlertDto(
        String  id,
        String  ticker,
        String  type,       // lowercase: above | below | change_up | change_down
        Double  value,
        Instant firedAt,
        Double  firedPrice,
        Instant createdAt
) {

    public static AlertDto from(Alert e) {
        return new AlertDto(
                e.getId(),
                e.getTicker(),
                e.getType().toString(),
                e.getValue()      != null ? e.getValue().doubleValue()      : null,
                e.getFiredAt(),
                e.getFiredPrice() != null ? e.getFiredPrice().doubleValue() : null,
                e.getCreatedAt()
        );
    }
}
