package com.ak.binancepricetracker.scheduler;

import com.ak.binancepricetracker.client.BinanceAveragePriceResponse;
import com.ak.binancepricetracker.client.BinanceClient;
import com.ak.binancepricetracker.service.PriceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class PricePollingSchedulerTest {
    @Mock BinanceClient binanceClient;
    @Mock PriceService priceService;
    private PricePollingScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new PricePollingScheduler(binanceClient, priceService, "BNBUSDT");
    }

    @Test
    @DisplayName("сохраняет полученную цену с переданным символом")
    void savesFetchedPrice() {
        when(binanceClient.getAveragePrice("BNBUSDT"))
                .thenReturn(new BinanceAveragePriceResponse(5, new BigDecimal("612.34"), 1762000000000L));

        scheduler.pollPrice();

        verify(binanceClient).getAveragePrice("BNBUSDT");
        ArgumentCaptor<OffsetDateTime> fetchedAtCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(priceService).savePrice(eq("BNBUSDT"), eq(new BigDecimal("612.34")), fetchedAtCaptor.capture());
        assertEquals(ZoneOffset.UTC, fetchedAtCaptor.getValue().getOffset());
    }

    @Test
    @DisplayName("подавляет ошибку Binance и не сохраняет цену")
    void handlesBinanceError() {
        when(binanceClient.getAveragePrice("BNBUSDT")).thenThrow(new RuntimeException("connection failed"));

        assertDoesNotThrow(() -> scheduler.pollPrice());

        verify(priceService, never()).savePrice(anyString(), any(), any());
    }

    @Test
    @DisplayName("подавляет пустой ответ Binance и не сохраняет цену")
    void handlesEmptyBinanceResponse() {
        when(binanceClient.getAveragePrice("BNBUSDT")).thenReturn(null);

        assertDoesNotThrow(() -> scheduler.pollPrice());

        verify(priceService, never()).savePrice(anyString(), any(), any());
    }

    @Test
    @DisplayName("подавляет ошибку базы данных после вызова сохранения")
    void handlesDatabaseError() {
        when(binanceClient.getAveragePrice("BNBUSDT"))
                .thenReturn(new BinanceAveragePriceResponse(5, new BigDecimal("612.34"), 1762000000000L));
        //удобен для настройки исключений при вызове метода, когда не хочется использовать обычную конструкцию when.
        doThrow(new RuntimeException("database unavailable"))
                .when(priceService).savePrice(eq("BNBUSDT"), eq(new BigDecimal("612.34")), any());

        assertDoesNotThrow(() -> scheduler.pollPrice());

        verify(priceService).savePrice(eq("BNBUSDT"), eq(new BigDecimal("612.34")), any());
    }

    @Test
    @DisplayName("продолжает опрос после ошибки базы данных")
    void continuesPollingAfterDatabaseError() {
        when(binanceClient.getAveragePrice("BNBUSDT"))
                .thenReturn(new BinanceAveragePriceResponse(5, new BigDecimal("612.34"), 1762000000000L));
        //задаёт последовательное поведение: первый вызов выбрасывает исключение, второй возвращает результат.
        when(priceService.savePrice(eq("BNBUSDT"), eq(new BigDecimal("612.34")), any()))
                .thenThrow(new RuntimeException("database unavailable"))
                .thenReturn(null);

        scheduler.pollPrice();
        scheduler.pollPrice();

        verify(binanceClient, times(2)).getAveragePrice("BNBUSDT");
        verify(priceService, times(2))
                .savePrice(eq("BNBUSDT"), eq(new BigDecimal("612.34")), any());
    }
}
