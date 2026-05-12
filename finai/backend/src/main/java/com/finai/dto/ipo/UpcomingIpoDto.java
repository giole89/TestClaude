package com.finai.dto.ipo;

/** IPO in arrivo dal calendario NASDAQ. */
public record UpcomingIpoDto(
        String id,
        String company,
        String ticker,
        String expectedDate,
        String priceRange,
        String shares,
        String exchange,
        String sector
) {}
