package com.finai.dto.ipo;

/** IPO recente con performance rispetto al prezzo di quotazione. */
public record RecentIpoDto(
        String id,
        String companyName,
        String ticker,
        String ipoDate,
        Double ipoPrice,
        Double currentPrice,
        /** Variazione % rispetto al prezzo IPO. Null se currentPrice non disponibile. */
        Double performance,
        String exchange
) {}
