package com.finai.dto.ipo;

/** IPO in arrivo dal calendario NASDAQ. */
public record UpcomingIpoDto(
        String id,
        String companyName,
        String proposedTicker,
        String expectedDate,
        String priceRange,
        String sharesOffered,
        String exchange,
        String dealStatus
) {}
