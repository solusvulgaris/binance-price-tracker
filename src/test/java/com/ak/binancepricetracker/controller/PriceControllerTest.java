package com.ak.binancepricetracker.controller;

import com.ak.binancepricetracker.entity.PriceHistory;
import com.ak.binancepricetracker.service.PriceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PriceController.class)
class PriceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PriceService priceService;

    @Test
    @DisplayName("возвращает последнюю цену с HTTP 200")
    void returnsLatestPrice() throws Exception {
        OffsetDateTime originalTime = OffsetDateTime.parse("2026-11-03T22:15:30+02:00");
        when(priceService.getLatestPrice("BNBUSDT")).thenReturn(Optional.of(
                new PriceHistory("BNBUSDT", new BigDecimal("612.45000000"), originalTime)));

        mockMvc.perform(get("/api/v1/prices/latest").param("symbol", "BNBUSDT"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "symbol": "BNBUSDT",
                          "price": 612.45000000,
                          "fetchedAt": "2026-11-03T20:15:30Z"
                        }
                        """));
    }

    @Test
    @DisplayName("возвращает HTTP 404, если цена не найдена")
    void returnsNotFoundWhenPriceMissing() throws Exception {
        when(priceService.getLatestPrice("BNBUSDT")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/prices/latest").param("symbol", "BNBUSDT"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No price found for symbol: BNBUSDT"));
    }

    @Test
    @DisplayName("преобразует время цены в UTC")
    void convertsFetchedAtToUtc() throws Exception {
        when(priceService.getLatestPrice("BTCUSDT")).thenReturn(Optional.of(
                new PriceHistory("BTCUSDT", BigDecimal.TEN,
                        OffsetDateTime.parse("2026-11-03T23:15:30+03:00"))));

        mockMvc.perform(get("/api/v1/prices/latest").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fetchedAt").value("2026-11-03T20:15:30Z"));
    }

    @Test
    @DisplayName("возвращает среднюю цену за период с HTTP 200")
    void returnsAveragePrice() throws Exception {
        OffsetDateTime from = OffsetDateTime.parse("2026-11-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-11-04T00:00:00Z");
        when(priceService.getAveragePrice("BNBUSDT", from, to))
                .thenReturn(Optional.of(new BigDecimal("612.45000000")));

        mockMvc.perform(get("/api/v1/prices/average")
                        .param("symbol", "BNBUSDT")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "symbol": "BNBUSDT",
                          "averagePrice": 612.45000000,
                          "from": "2026-11-01T00:00:00Z",
                          "to": "2026-11-04T00:00:00Z"
                        }
                        """));
    }

    @Test
    @DisplayName("возвращает HTTP 404, если средней цены за период нет")
    void returnsNotFoundWhenAveragePriceMissing() throws Exception {
        OffsetDateTime from = OffsetDateTime.parse("2026-11-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-11-04T00:00:00Z");
        when(priceService.getAveragePrice("BNBUSDT", from, to)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/prices/average")
                        .param("symbol", "BNBUSDT")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No average price found for symbol: BNBUSDT"));
    }

    @Test
    @DisplayName("возвращает HTTP 400, если from позже to")
    void returnsBadRequestForReversedPeriod() throws Exception {
        OffsetDateTime from = OffsetDateTime.parse("2026-11-04T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-11-01T00:00:00Z");
        when(priceService.getAveragePrice("BNBUSDT", from, to))
                .thenThrow(new IllegalArgumentException("from must not be after to"));

        mockMvc.perform(get("/api/v1/prices/average")
                        .param("symbol", "BNBUSDT")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("возвращает HTTP 400 при некорректном формате даты")
    void returnsBadRequestForInvalidDate() throws Exception {
        mockMvc.perform(get("/api/v1/prices/average")
                        .param("symbol", "BNBUSDT")
                        .param("from", "invalid-date")
                        .param("to", "2026-11-04T00:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("возвращает HTTP 400 при отсутствии обязательного параметра")
    void returnsBadRequestWhenParameterMissing() throws Exception {
        mockMvc.perform(get("/api/v1/prices/average")
                        .param("symbol", "BNBUSDT")
                        .param("from", "2026-11-01T00:00:00Z"))
                .andExpect(status().isBadRequest());
    }
}
