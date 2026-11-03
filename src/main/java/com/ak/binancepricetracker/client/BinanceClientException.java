package com.ak.binancepricetracker.client;

/** Ошибка обращения к Binance или обработки ответа Binance. */
public class BinanceClientException extends RuntimeException {

    public BinanceClientException(String message) {
        super(message);
    }

    public BinanceClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
