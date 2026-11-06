package com.ak.binancepricetracker.controller;

import com.ak.binancepricetracker.entity.PriceHistory;
import com.ak.binancepricetracker.service.PriceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = PriceController.class, properties = "binance.symbol=BNBUSDT")
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
                new PriceHistory("BNBUSDT", new BigDecimal("612.45000000"), originalTime,
                        originalTime.toInstant().toEpochMilli())));

        mockMvc.perform(get("/api/v1/prices/latest").param("symbol", "BNBUSDT"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "symbol": "BNBUSDT",
                          "price": 612.45000000,
                          "fetchedAt": "2026-11-03T20:15:30Z"
                        }
                        """, JsonCompareMode.STRICT));
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
                        OffsetDateTime.parse("2026-11-03T23:15:30+03:00"), 1793736930000L)));

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
                          "averagePrice": 612.45000000
                        }
                        """, JsonCompareMode.STRICT));
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

    @Test
    @DisplayName("возвращает страницу истории с метаданными")
    void returnsHistoryPage() throws Exception {
        OffsetDateTime from = OffsetDateTime.parse("2026-09-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-09-30T23:59:59Z");
        when(priceService.getPriceHistory("BNBUSDT", from, to,
                org.springframework.data.domain.PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(new PriceHistory(
                        "BNBUSDT", new BigDecimal("600.25"), from, from.toInstant().toEpochMilli())),
                        org.springframework.data.domain.PageRequest.of(0, 20), 135));
        mockMvc.perform(get("/api/v1/prices").param("symbol", "BNBUSDT")
                        .param("from", from.toString()).param("to", to.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].symbol").value("BNBUSDT"))
                .andExpect(jsonPath("$.items[0].price").value(600.25))
                .andExpect(jsonPath("$.items[0].fetchedAt").value("2026-09-01T00:00:00Z"))
                .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(135)).andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    @DisplayName("возвращает пустую страницу истории")
    void returnsEmptyHistoryPage() throws Exception {
        OffsetDateTime from = OffsetDateTime.parse("2026-09-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-09-30T23:59:59Z");
        when(priceService.getPriceHistory("BNBUSDT", from, to,
                org.springframework.data.domain.PageRequest.of(0, 20))).thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/api/v1/prices").param("symbol", "BNBUSDT")
                        .param("from", from.toString()).param("to", to.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0)).andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @DisplayName("отклоняет некорректные параметры пагинации и интервал")
    void rejectsInvalidHistoryParameters() throws Exception {
        String endpoint = "/api/v1/prices?symbol=BNBUSDT&from=2026-09-30T00:00:00Z&to=2026-09-01T00:00:00Z";
        mockMvc.perform(get(endpoint).param("page", "-1")).andExpect(status().isBadRequest());
        mockMvc.perform(get(endpoint).param("size", "0")).andExpect(status().isBadRequest());
        mockMvc.perform(get(endpoint).param("size", "101")).andExpect(status().isBadRequest());
        when(priceService.getPriceHistory(eq("BNBUSDT"), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("from must not be after to"));
        mockMvc.perform(get("/api/v1/prices").param("symbol", "BNBUSDT")
                        .param("from", "2026-09-30T00:00:00Z").param("to", "2026-09-01T00:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("возвращает тренд вверх, вниз и без изменений")
    void returnsTrends() throws Exception {
        OffsetDateTime from = OffsetDateTime.parse("2026-09-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-09-30T23:59:59Z");
        var scenarios = List.of(
                new Object[]{BigDecimal.ONE, new BigDecimal("2"), "UPWARD"},
                new Object[]{new BigDecimal("2"), BigDecimal.ONE, "DOWNWARD"},
                new Object[]{BigDecimal.ONE, BigDecimal.ONE, "FLAT"});
        for (var scenario : scenarios) {
            when(priceService.getFirstAndLastPrices("BNBUSDT", from, to)).thenReturn(Optional.of(
                    new PriceService.PricePeriod(
                            new PriceHistory("BNBUSDT", (BigDecimal) scenario[0], from,
                                    from.toInstant().toEpochMilli()),
                            new PriceHistory("BNBUSDT", (BigDecimal) scenario[1], to,
                                    to.toInstant().toEpochMilli()))));
            mockMvc.perform(get("/api/v1/prices/trend").param("symbol", "BNBUSDT")
                            .param("from", from.toString()).param("to", to.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.trend").value(scenario[2]))
                    .andExpect(jsonPath("$.symbol").value("BNBUSDT"))
                    .andExpect(jsonPath("$.start_date").value("2026-09-01"))
                    .andExpect(jsonPath("$.end_date").value("2026-09-30"));
        }
    }

    @Test
    @DisplayName("возвращает HTTP 404 для тренда без данных")
    void returnsNotFoundForTrendWithoutData() throws Exception {
        OffsetDateTime from = OffsetDateTime.parse("2026-09-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-09-30T23:59:59Z");
        when(priceService.getFirstAndLastPrices("BNBUSDT", from, to)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/prices/trend").param("symbol", "BNBUSDT")
                        .param("from", from.toString()).param("to", to.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("возвращает HTTP 400 для тренда с некорректным интервалом")
    void rejectsInvalidTrendInterval() throws Exception {
        OffsetDateTime from = OffsetDateTime.parse("2026-09-30T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-09-01T00:00:00Z");
        when(priceService.getFirstAndLastPrices("BNBUSDT", from, to))
                .thenThrow(new IllegalArgumentException("from must not be after to"));
        mockMvc.perform(get("/api/v1/prices/trend").param("symbol", "BNBUSDT")
                        .param("from", from.toString()).param("to", to.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("возвращает HTTP 400 для тренда без обязательного параметра")
    void rejectsMissingTrendParameter() throws Exception {
        mockMvc.perform(get("/api/v1/prices/trend").param("symbol", "BNBUSDT")
                        .param("from", "2026-09-01T00:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("возвращает HTTP 400 для истории без обязательного параметра")
    void rejectsMissingHistoryParameter() throws Exception {
        mockMvc.perform(get("/api/v1/prices").param("symbol", "BNBUSDT")
                        .param("from", "2026-09-01T00:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("использует символ по умолчанию для latest")
    void usesDefaultSymbolForLatest() throws Exception {
        when(priceService.getLatestPrice("BNBUSDT")).thenReturn(Optional.of(
                new PriceHistory("BNBUSDT", BigDecimal.ONE,
                        OffsetDateTime.parse("2026-11-01T00:00:00Z"), 1793491200000L)));
        mockMvc.perform(get("/api/v1/prices/latest"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.symbol").value("BNBUSDT"));
    }

    @Test
    @DisplayName("использует символ по умолчанию для average")
    void usesDefaultSymbolForAverage() throws Exception {
        OffsetDateTime from = OffsetDateTime.parse("2026-11-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-11-02T00:00:00Z");
        when(priceService.getAveragePrice("BNBUSDT", from, to)).thenReturn(Optional.of(BigDecimal.ONE));
        mockMvc.perform(get("/api/v1/prices/average").param("from", from.toString()).param("to", to.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.averagePrice").value(1));
    }

    @Test
    @DisplayName("использует символ по умолчанию для истории")
    void usesDefaultSymbolForHistory() throws Exception {
        OffsetDateTime from = OffsetDateTime.parse("2026-11-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-11-02T00:00:00Z");
        when(priceService.getPriceHistory(eq("BNBUSDT"), eq(from), eq(to), any()))
                .thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/api/v1/prices").param("from", from.toString()).param("to", to.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("использует символ по умолчанию для тренда")
    void usesDefaultSymbolForTrend() throws Exception {
        OffsetDateTime from = OffsetDateTime.parse("2026-11-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-11-02T00:00:00Z");
        when(priceService.getFirstAndLastPrices("BNBUSDT", from, to)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/prices/trend").param("from", from.toString()).param("to", to.toString()))
                .andExpect(status().isNotFound());
    }
}
