package com.ak.binancepricetracker.client;

/** Ошибка обращения к Binance или обработки ответа Binance. */
public class BinanceClientException extends RuntimeException {

    private final Integer statusCode;
    private final String retryAfter;

    public BinanceClientException(String message) {
        this(message, null, null, null);
    }

    public BinanceClientException(String message, Throwable cause) {
        this(message, cause, null, null);
    }

    public BinanceClientException(String message, int statusCode, String retryAfter) {
        this(message, null, statusCode, retryAfter);
    }

    private BinanceClientException(String message, Throwable cause, Integer statusCode, String retryAfter) {
        super(message, cause);
        this.statusCode = statusCode;
        this.retryAfter = retryAfter;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getRetryAfter() {
        return retryAfter;
    }
}
