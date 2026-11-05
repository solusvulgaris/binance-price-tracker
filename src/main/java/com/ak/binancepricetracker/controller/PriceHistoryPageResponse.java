package com.ak.binancepricetracker.controller;

import com.ak.binancepricetracker.entity.PriceHistory;
import org.springframework.data.domain.Page;

import java.util.List;

public record PriceHistoryPageResponse(
        List<PriceHistoryItemResponse> items,
        int page,
        int size,
        long totalElements,
        boolean hasNext
) {
    public static PriceHistoryPageResponse from(Page<PriceHistory> prices) {
        return new PriceHistoryPageResponse(
                prices.getContent().stream().map(PriceHistoryItemResponse::from).toList(),
                prices.getNumber(), prices.getSize(), prices.getTotalElements(), prices.hasNext());
    }
}
