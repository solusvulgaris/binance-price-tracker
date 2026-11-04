package com.ak.binancepricetracker;

import com.ak.binancepricetracker.client.BinanceAveragePriceResponse;
import com.ak.binancepricetracker.client.BinanceClient;
import com.ak.binancepricetracker.service.PriceService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.mockito.Mockito.when;

@SpringBootTest(properties = "binance.poll-initial-delay=PT1H")
@Testcontainers
class BinancepricetrackerApplicationTests {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:14-alpine");

    @Autowired
    private PriceService priceService;

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

        priceService.savePrice("BNBUSDT", new BigDecimal("612.45"), fetchedAt);

        var loaded = priceService.getLatestPrice("BNBUSDT").orElseThrow();
        Assertions.assertAll(
                () -> Assertions.assertEquals("BNBUSDT", loaded.getSymbol()),
                () -> Assertions.assertEquals(0, loaded.getPrice().compareTo(new BigDecimal("612.45"))),
                () -> Assertions.assertEquals(fetchedAt, loaded.getFetchedAt())
        );
    }

}
