package com.ak.binancepricetracker.scheduler;

import com.ak.binancepricetracker.client.BinanceAveragePriceResponse;
import com.ak.binancepricetracker.client.BinanceClient;
import com.ak.binancepricetracker.service.PriceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class PricePollingScheduler {

    private static final Logger log = LoggerFactory.getLogger(PricePollingScheduler.class);

    private final BinanceClient binanceClient;
    private final PriceService priceService;
    private final String symbol;

    public PricePollingScheduler(BinanceClient binanceClient,
                                 PriceService priceService,
                                 @Value("${binance.symbol}") String symbol) {
        this.binanceClient = binanceClient;
        this.priceService = priceService;
        this.symbol = symbol;
    }

    @Scheduled(
            initialDelayString = "${binance.poll-initial-delay:PT0S}",
            fixedDelayString = "${binance.poll-interval}")
    public void pollPrice() {
        try {
            BinanceAveragePriceResponse response = binanceClient.getAveragePrice(symbol);
            if (response == null || response.price() == null || response.closeTime() == null) {
                throw new IllegalStateException("Binance returned an empty price response");
            }
            boolean saved = priceService.savePrice(
                    symbol, response.price(), OffsetDateTime.now(ZoneOffset.UTC), response.closeTime());
            if (saved) {
                log.info("Saved Binance price for {}: {}", symbol, response.price());
            } else {
                log.debug("Skipped duplicate Binance snapshot for {} at {}", symbol, response.closeTime());
            }
        } catch (Exception exception) {
            log.error("Failed to poll Binance price for {}", symbol, exception);
        }
    }
}
