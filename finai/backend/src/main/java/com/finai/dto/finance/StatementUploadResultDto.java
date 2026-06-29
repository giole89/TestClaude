package com.finai.dto.finance;

import java.util.List;

/** Risposta dell'import di un estratto conto: quanti movimenti sono stati riconosciuti. */
public record StatementUploadResultDto(int imported, int skipped, int duplicates, List<TransactionDto> transactions) {}
