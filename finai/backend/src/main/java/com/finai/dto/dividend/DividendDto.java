package com.finai.dto.dividend;

/** Dati dividendi per un ticker. */
public record DividendDto(
        String ticker,
        String companyName,
        /** Dividendo annuale per azione (trailing). */
        Double annualDividend,
        /** Rendimento dividendo (es. 0.03 = 3%). */
        Double dividendYield,
        /** Data ex-dividendo formattata (yyyy-MM-dd). */
        String exDividendDate,
        /** Data pagamento dividendo formattata (yyyy-MM-dd). */
        String dividendDate,
        /** Frequenza stimata pagamenti (1=annuale, 2=semestrale, 4=trimestrale, 12=mensile). */
        Integer payFrequency
) {}
