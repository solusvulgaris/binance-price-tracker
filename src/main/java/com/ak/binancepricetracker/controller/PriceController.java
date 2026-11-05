package com.ak.binancepricetracker.controller;

import com.ak.binancepricetracker.service.PriceService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/prices")
public class PriceController {

    private final PriceService priceService;
    private final String defaultSymbol;

    public PriceController(PriceService priceService, @Value("${binance.symbol:BNBUSDT}") String defaultSymbol) {
        this.priceService = priceService;
        this.defaultSymbol = defaultSymbol;
    }

    @GetMapping("/latest")
    public ResponseEntity<Object> getLatestPrice(@RequestParam(required = false) String symbol) {
        String resolvedSymbol = resolveSymbol(symbol);
        return priceService.getLatestPrice(resolvedSymbol)
                .<ResponseEntity<Object>>map(price -> ResponseEntity.ok(LatestPriceResponse.from(price)))
                .orElseGet(() -> ResponseEntity.status(404)
                        .body("No price found for symbol: " + resolvedSymbol));
    }

    @GetMapping("/average")
    public ResponseEntity<Object> getAveragePrice(
            @RequestParam(required = false) String symbol,
            @RequestParam OffsetDateTime from,
            @RequestParam OffsetDateTime to) {
        try {
            String resolvedSymbol = resolveSymbol(symbol);
            return priceService.getAveragePrice(resolvedSymbol, from, to)
                    .<ResponseEntity<Object>>map(average -> ResponseEntity.ok(
                            new AveragePriceResponse(average)))
                    .orElseGet(() -> ResponseEntity.status(404)
                            .body("No average price found for symbol: " + resolvedSymbol));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<Object> getPriceHistory(
            @RequestParam(required = false) String symbol, @RequestParam OffsetDateTime from, @RequestParam OffsetDateTime to,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            return ResponseEntity.badRequest().body("page must be non-negative and size must be between 1 and 100");
        }
        try {
            String resolvedSymbol = resolveSymbol(symbol);
            return ResponseEntity.ok(PriceHistoryPageResponse.from(
                    priceService.getPriceHistory(resolvedSymbol, from, to, PageRequest.of(page, size))));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @GetMapping("/trend")
    public ResponseEntity<Object> getPriceTrend(
            @RequestParam(required = false) String symbol, @RequestParam OffsetDateTime from, @RequestParam OffsetDateTime to) {
        try {
            String resolvedSymbol = resolveSymbol(symbol);
            return priceService.getFirstAndLastPrices(resolvedSymbol, from, to)
                    .<ResponseEntity<Object>>map(period -> ResponseEntity.ok(PriceTrendResponse.from(resolvedSymbol, period)))
                    .orElseGet(() -> ResponseEntity.status(404)
                            .body("No price data found for symbol: " + resolvedSymbol));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    private String resolveSymbol(String symbol) {
        return symbol == null || symbol.isBlank() ? defaultSymbol : symbol;
    }
}
