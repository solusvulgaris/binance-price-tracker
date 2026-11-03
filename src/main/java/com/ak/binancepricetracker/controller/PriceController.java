package com.ak.binancepricetracker.controller;

import com.ak.binancepricetracker.service.PriceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
