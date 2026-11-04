package com.ak.binancepricetracker.controller;

import com.ak.binancepricetracker.service.PriceService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/prices")
public class PriceController {

    private final PriceService priceService;

    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    @GetMapping("/latest")
    public ResponseEntity<Object> getLatestPrice(@RequestParam String symbol) {
        return priceService.getLatestPrice(symbol)
                .<ResponseEntity<Object>>map(price -> ResponseEntity.ok(LatestPriceResponse.from(price)))
                .orElseGet(() -> ResponseEntity.status(404)
                        .body("No price found for symbol: " + symbol));
    }

    @GetMapping("/average")
    public ResponseEntity<Object> getAveragePrice(
            @RequestParam String symbol,
            @RequestParam OffsetDateTime from,
            @RequestParam OffsetDateTime to) {
        try {
            return priceService.getAveragePrice(symbol, from, to)
                    .<ResponseEntity<Object>>map(average -> ResponseEntity.ok(
                            AveragePriceResponse.of(symbol, average, from, to)))
                    .orElseGet(() -> ResponseEntity.status(404)
                            .body("No average price found for symbol: " + symbol));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<Object> getPriceHistory(
            @RequestParam String symbol, @RequestParam OffsetDateTime from, @RequestParam OffsetDateTime to,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            return ResponseEntity.badRequest().body("page must be non-negative and size must be between 1 and 100");
        }
        try {
            return ResponseEntity.ok(PriceHistoryPageResponse.from(
                    priceService.getPriceHistory(symbol, from, to, PageRequest.of(page, size))));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @GetMapping("/trend")
    public ResponseEntity<Object> getPriceTrend(
            @RequestParam String symbol, @RequestParam OffsetDateTime from, @RequestParam OffsetDateTime to) {
        try {
            return priceService.getFirstAndLastPrices(symbol, from, to)
                    .<ResponseEntity<Object>>map(period -> ResponseEntity.ok(PriceTrendResponse.from(symbol, period)))
                    .orElseGet(() -> ResponseEntity.status(404)
                            .body("No price data found for symbol: " + symbol));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }
}
