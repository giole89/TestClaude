package com.finai.dto.finance;

import com.finai.domain.entity.BankTransaction;

import java.time.LocalDate;

/** DTO di risposta per un movimento importato da estratto conto. */
public record TransactionDto(
        String    id,
        LocalDate date,
        String    description,
        Double    amount,
        String    category,
        String    type,
        String    sourceFile
) {
    public static TransactionDto from(BankTransaction e) {
        return new TransactionDto(
                e.getId(), e.getTxDate(), e.getDescription(), e.getAmount().doubleValue(),
                e.getCategory(), e.getType(), e.getSourceFile()
        );
    }
}
