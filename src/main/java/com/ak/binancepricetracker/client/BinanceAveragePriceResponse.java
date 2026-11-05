package com.ak.binancepricetracker.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/** Ответ Binance для endpoint усреднённой цены. */
public record BinanceAveragePriceResponse(
        Integer mins,
        BigDecimal price,
        @JsonProperty("closeTime") Long closeTime
) {
}
