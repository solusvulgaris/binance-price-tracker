package com.ak.binancepricetracker.client;

import org.springframework.http.HttpStatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Objects;

/** Клиент публичного Binance API для получения средней цены торговой пары. */
@Component
public class BinanceClient {

    static final String BASE_URL = "https://api.binance.com";
    static final String AVERAGE_PRICE_PATH = "/api/v3/avgPrice";

    private final RestClient restClient;

    @Autowired
    public BinanceClient(RestClient.Builder restClientBuilder) {
        this(restClientBuilder, BASE_URL);
    }

    BinanceClient(RestClient.Builder restClientBuilder, String baseUrl) {
        this.restClient = Objects.requireNonNull(restClientBuilder, "restClientBuilder must not be null")
                .baseUrl(Objects.requireNonNull(baseUrl, "baseUrl must not be null"))
                .build();
    }

    public BinanceAveragePriceResponse getAveragePrice(String symbol) {
        validateSymbol(symbol);
        try {
            BinanceAveragePriceResponse priceResponse = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(AVERAGE_PRICE_PATH)
                            .queryParam("symbol", symbol)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new BinanceClientException(
                                "Binance returned HTTP status " + response.getStatusCode().value());
                    })
                    .body(BinanceAveragePriceResponse.class);

            if (priceResponse == null
                    || priceResponse.price() == null
                    || priceResponse.price().signum() <= 0) {
                throw new BinanceClientException("Binance response contains invalid price");
            }
            return priceResponse;
        } catch (RestClientException exception) {
            throw new BinanceClientException("Could not get average price from Binance", exception);
        }
    }

    private void validateSymbol(String symbol) {
        Objects.requireNonNull(symbol, "symbol must not be null");
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
    }
}
