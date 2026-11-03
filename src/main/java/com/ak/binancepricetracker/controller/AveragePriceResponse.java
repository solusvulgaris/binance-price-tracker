package com.ak.binancepricetracker.controller;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record AveragePriceResponse(
        String symbol,
        BigDecimal averagePrice,
        OffsetDateTime from,
        OffsetDateTime to
) {

    public static AveragePriceResponse of(
            String symbol, BigDecimal averagePrice, OffsetDateTime from, OffsetDateTime to) {
        return new AveragePriceResponse(
                symbol,
                averagePrice,
                from.withOffsetSameInstant(ZoneOffset.UTC),
                to.withOffsetSameInstant(ZoneOffset.UTC));
    }
}
