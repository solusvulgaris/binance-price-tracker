package com.ak.binancepricetracker.controller;

import com.ak.binancepricetracker.entity.PriceHistory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record LatestPriceResponse(String symbol, BigDecimal price, OffsetDateTime fetchedAt) {

    public static LatestPriceResponse from(PriceHistory priceHistory) {
        return new LatestPriceResponse(
                priceHistory.getSymbol(),
                priceHistory.getPrice(),
                priceHistory.getFetchedAt().withOffsetSameInstant(ZoneOffset.UTC));
    }
}
