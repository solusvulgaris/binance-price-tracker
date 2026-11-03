package com.ak.binancepricetracker.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class BinanceClientTest {

    private final RestClient.Builder restClientBuilder = RestClient.builder().baseUrl("http://binance.test");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
    private final BinanceClient client = new BinanceClient(restClientBuilder, "http://binance.test");

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    @DisplayName("получает среднюю цену и преобразует price в BigDecimal")
    void getsAveragePrice() {
        server.expect(requestTo("http://binance.test/api/v3/avgPrice?symbol=BNBUSDT"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"mins\":5,\"price\":\"612.34000000\",\"closeTime\":1762000000000}",
                        org.springframework.http.MediaType.APPLICATION_JSON));

        BinanceAveragePriceResponse response = client.getAveragePrice("BNBUSDT");

        assertEquals(5, response.mins());
        assertEquals(new BigDecimal("612.34000000"), response.price());
        assertEquals(1762000000000L, response.closeTime());
    }

    @Test
    @DisplayName("обрабатывает HTTP 4xx ошибку Binance")
    void handlesClientError() {
        server.expect(requestTo("http://binance.test/api/v3/avgPrice?symbol=BNBUSDT"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));
        assertThrows(BinanceClientException.class, () -> client.getAveragePrice("BNBUSDT"));
    }

    @Test
    @DisplayName("обрабатывает HTTP 5xx ошибку Binance")
    void handlesServerError() {
        server.expect(requestTo("http://binance.test/api/v3/avgPrice?symbol=BNBUSDT"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        assertThrows(BinanceClientException.class, () -> client.getAveragePrice("BNBUSDT"));
    }

    @Test
    @DisplayName("обрабатывает некорректный JSON")
    void handlesMalformedJson() {
        server.expect(requestTo("http://binance.test/api/v3/avgPrice?symbol=BNBUSDT"))
                .andRespond(withSuccess("{not-json}", org.springframework.http.MediaType.APPLICATION_JSON));
        assertThrows(BinanceClientException.class, () -> client.getAveragePrice("BNBUSDT"));
    }

    @Test
    @DisplayName("обрабатывает ответ без цены")
    void handlesMissingPrice() {
        server.expect(requestTo("http://binance.test/api/v3/avgPrice?symbol=BNBUSDT"))
                .andRespond(withSuccess("{\"mins\":5,\"closeTime\":1762000000000}",
                        org.springframework.http.MediaType.APPLICATION_JSON));
        assertThrows(BinanceClientException.class, () -> client.getAveragePrice("BNBUSDT"));
    }

    @Test
    @DisplayName("отклоняет нулевую цену")
    void handlesZeroPrice() {
        server.expect(requestTo("http://binance.test/api/v3/avgPrice?symbol=BNBUSDT"))
                .andRespond(withSuccess("{\"mins\":5,\"price\":\"0\",\"closeTime\":1762000000000}",
                        org.springframework.http.MediaType.APPLICATION_JSON));
        assertThrows(BinanceClientException.class, () -> client.getAveragePrice("BNBUSDT"));
    }

    @Test
    @DisplayName("оборачивает ошибку соединения")
    void handlesConnectionError() {
        BinanceClient unavailableClient = new BinanceClient(
                RestClient.builder(), "http://localhost:1");
        assertThrows(BinanceClientException.class, () -> unavailableClient.getAveragePrice("BNBUSDT"));
    }
}
