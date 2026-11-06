package com.ak.binancepricetracker;

import com.ak.binancepricetracker.client.BinanceAveragePriceResponse;
import com.ak.binancepricetracker.client.BinanceClient;
import com.ak.binancepricetracker.repository.PriceHistoryRepository;
import com.ak.binancepricetracker.service.PriceService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.when;

@SpringBootTest(properties = "binance.poll-initial-delay=PT1H")
@Testcontainers
class BinancepricetrackerApplicationTests {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:14-alpine");

    @Autowired
    private PriceService priceService;

    @Autowired
    private PriceHistoryRepository repository;

    @MockitoBean
    private BinanceClient binanceClient;

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void configureBinanceClient() {
        repository.deleteAll();
        when(binanceClient.getAveragePrice("BNBUSDT"))
                .thenReturn(new BinanceAveragePriceResponse(
                        5, new BigDecimal("600.00"), 1762000000000L));
    }

    @Test
    @DisplayName("загружает контекст приложения")
    void contextLoads() {
    }

    @Test
    @DisplayName("сохраняет и читает запись через PostgreSQL и JPA")
    void savesAndReadsPriceHistory() {
        OffsetDateTime fetchedAt = OffsetDateTime.parse("2026-11-04T10:15:30Z");
        long closeTime = fetchedAt.toInstant().toEpochMilli();

        priceService.savePrice("BNBUSDT", new BigDecimal("612.45"), fetchedAt, closeTime);

        var loaded = priceService.getLatestPrice("BNBUSDT").orElseThrow();
        Assertions.assertAll(
                () -> Assertions.assertEquals("BNBUSDT", loaded.getSymbol()),
                () -> Assertions.assertEquals(0, loaded.getPrice().compareTo(new BigDecimal("612.45"))),
                () -> Assertions.assertEquals(fetchedAt, loaded.getFetchedAt()),
                () -> Assertions.assertEquals(closeTime, loaded.getCloseTime())
        );
    }

    @Test
    @DisplayName("повторное сохранение снимка не создаёт дубликат")
    void doesNotDuplicateSnapshot() {
        long closeTime = OffsetDateTime.parse("2026-11-04T10:15:30Z").toInstant().toEpochMilli();

        OffsetDateTime firstFetch = OffsetDateTime.parse("2026-11-04T10:15:31Z");
        OffsetDateTime secondFetch = OffsetDateTime.parse("2026-11-04T10:15:32Z");

        Assertions.assertTrue(priceService.savePrice(
                "BNBUSDT", new BigDecimal("612.45"), firstFetch, closeTime));
        Assertions.assertFalse(priceService.savePrice(
                "BNBUSDT", new BigDecimal("612.45"), secondFetch, closeTime));

        Assertions.assertEquals(1, repository.count());
    }

    @Test
    @DisplayName("снимки с разными closeTime сохраняются и доступны в истории")
    void savesDifferentSnapshotsAndReturnsHistory() {
        OffsetDateTime firstTime = OffsetDateTime.parse("2026-11-04T10:15:30Z");
        OffsetDateTime secondTime = OffsetDateTime.parse("2026-11-04T10:16:30Z");

        Assertions.assertTrue(priceService.savePrice(
                "BNBUSDT", new BigDecimal("612.45"), firstTime, 1762251330000L));
        Assertions.assertTrue(priceService.savePrice(
                "BNBUSDT", new BigDecimal("613.10"), secondTime, 1762251390000L));

        var history = priceService.getPriceHistory(
                "BNBUSDT", firstTime, secondTime, PageRequest.of(0, 20));
        Assertions.assertAll(
                () -> Assertions.assertEquals(2, repository.count()),
                () -> Assertions.assertEquals(2, history.getTotalElements()),
                () -> Assertions.assertEquals(firstTime, history.getContent().get(0).getFetchedAt()),
                () -> Assertions.assertEquals(secondTime, history.getContent().get(1).getFetchedAt())
        );
    }

    @Test
    @DisplayName("конкурентное сохранение одного снимка создаёт одну запись")
    void concurrentlySavesSnapshotOnce() throws Exception {
        long closeTime = 1762251330000L;
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> {
                ready.countDown();
                start.await();
                return priceService.savePrice("BNBUSDT", new BigDecimal("612.45"),
                        OffsetDateTime.parse("2026-11-04T10:15:31Z"), closeTime);
            });
            var second = executor.submit(() -> {
                ready.countDown();
                start.await();
                return priceService.savePrice("BNBUSDT", new BigDecimal("612.45"),
                        OffsetDateTime.parse("2026-11-04T10:15:32Z"), closeTime);
            });

            Assertions.assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            boolean firstResult = first.get(5, TimeUnit.SECONDS);
            boolean secondResult = second.get(5, TimeUnit.SECONDS);
            Assertions.assertAll(
                    () -> Assertions.assertTrue(firstResult ^ secondResult),
                    () -> Assertions.assertEquals(1, repository.count())
            );
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

}
