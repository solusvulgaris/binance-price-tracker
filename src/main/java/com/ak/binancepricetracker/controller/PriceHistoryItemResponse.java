package com.ak.binancepricetracker.controller;

import com.ak.binancepricetracker.entity.PriceHistory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record PriceHistoryItemResponse(String symbol, BigDecimal price, OffsetDateTime fetchedAt) {
    public static PriceHistoryItemResponse from(PriceHistory price) {
        return new PriceHistoryItemResponse(price.getSymbol(), price.getPrice(),
                price.getFetchedAt().withOffsetSameInstant(ZoneOffset.UTC));
    }
}
