package com.ak.binancepricetracker.controller;

import com.ak.binancepricetracker.service.PriceService;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZoneOffset;

public record PriceTrendResponse(
        String symbol,
        @JsonProperty("start_date") String startDate,
        @JsonProperty("end_date") String endDate,
        Trend trend) {
    public enum Trend { UPWARD, DOWNWARD, FLAT }

    public static PriceTrendResponse from(String symbol, PriceService.PricePeriod period) {
        int comparison = period.last().getPrice().compareTo(period.first().getPrice());
        Trend trend = comparison > 0 ? Trend.UPWARD : comparison < 0 ? Trend.DOWNWARD : Trend.FLAT;
        return new PriceTrendResponse(symbol,
                period.first().getFetchedAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDate().toString(),
                period.last().getFetchedAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDate().toString(),
                trend);
    }
}
