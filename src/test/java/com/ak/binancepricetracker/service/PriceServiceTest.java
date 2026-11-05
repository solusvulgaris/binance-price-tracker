package com.ak.binancepricetracker.service;

import com.ak.binancepricetracker.entity.PriceHistory;
import com.ak.binancepricetracker.repository.PriceHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceServiceTest {
    @Mock PriceHistoryRepository repository;
    private PriceService service;
    private final OffsetDateTime from = OffsetDateTime.parse("2025-01-01T00:00:00Z");
    private final OffsetDateTime to = OffsetDateTime.parse("2025-01-02T00:00:00Z");

    @BeforeEach void setUp() { service = new PriceService(repository); }

    @Test
    @DisplayName("возвращает последнюю цену символа")
    void returnsLatestPrice() {
        PriceHistory price = new PriceHistory("BTCUSDT", new BigDecimal("100"), to);
        when(repository.findFirstBySymbolOrderByFetchedAtDescIdDesc("BTCUSDT")).thenReturn(Optional.of(price));
        assertSame(price, service.getLatestPrice("BTCUSDT").orElseThrow());
    }

    @Test
    @DisplayName("получает историю с пагинацией за заданный период")
    void delegatesHistoryQuery() {
        var pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(new PriceHistory("BTCUSDT", BigDecimal.TEN, from)));
        when(repository.findBySymbolAndFetchedAtBetweenOrderByFetchedAtAscIdAsc("BTCUSDT", from, to, pageable))
                .thenReturn(page);
        assertSame(page, service.getPriceHistory("BTCUSDT", from, to, pageable));
    }

    @Test
    @DisplayName("принимает интервал с одинаковыми from и to")
    void acceptsEqualInterval() {
        var pageable = PageRequest.of(0, 10);
        var page = new PageImpl<PriceHistory>(List.of());
        when(repository.findBySymbolAndFetchedAtBetweenOrderByFetchedAtAscIdAsc("BTCUSDT", from, from, pageable))
                .thenReturn(page);
        assertSame(page, service.getPriceHistory("BTCUSDT", from, from, pageable));
    }

    @Test
    @DisplayName("возвращает пустую историю, если записей нет")
    void returnsEmptyHistory() {
        var pageable = PageRequest.of(0, 10);
        when(repository.findBySymbolAndFetchedAtBetweenOrderByFetchedAtAscIdAsc("BTCUSDT", from, to, pageable))
                .thenReturn(new PageImpl<>(List.of()));
        assertTrue(service.getPriceHistory("BTCUSDT", from, to, pageable).isEmpty());
    }

    @Test
    @DisplayName("возвращает среднюю цену за период")
    void returnsAveragePrice() {
        when(repository.findAveragePriceBySymbolAndFetchedAtBetween("BTCUSDT", from, to))
                .thenReturn(Optional.of(new BigDecimal("105.50")));
        assertEquals(new BigDecimal("105.50"), service.getAveragePrice("BTCUSDT", from, to).orElseThrow());
    }

    @Test
    @DisplayName("возвращает пустой результат средней цены, если данных нет")
    void returnsEmptyAverageWhenNoData() {
        when(repository.findAveragePriceBySymbolAndFetchedAtBetween("BTCUSDT", from, to))
                .thenReturn(Optional.empty());
        assertTrue(service.getAveragePrice("BTCUSDT", from, to).isEmpty());
    }

    @Test
    @DisplayName("возвращает первую и последнюю записи в корректном порядке")
    void returnsFirstAndLastPrices() {
        PriceHistory first = new PriceHistory("BTCUSDT", BigDecimal.TEN, from);
        PriceHistory last = new PriceHistory("BTCUSDT", new BigDecimal("20"), to);
        when(repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtAscIdAsc("BTCUSDT", from, to))
                .thenReturn(Optional.of(first));
        when(repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtDescIdDesc("BTCUSDT", from, to))
                .thenReturn(Optional.of(last));
        var period = service.getFirstAndLastPrices("BTCUSDT", from, to).orElseThrow();
        assertSame(first, period.first());
        assertSame(last, period.last());
    }

    @Test
    @DisplayName("отклоняет период, в котором from позже to")
    void rejectsReversedInterval() {
        assertThrows(IllegalArgumentException.class, () -> service.getAveragePrice("BTCUSDT", to, from));
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("отклоняет null, пустой, пробельный и некорректный символ")
    void rejectsInvalidSymbol() {
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> service.getLatestPrice(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> service.getLatestPrice("")),
                () -> assertThrows(IllegalArgumentException.class, () -> service.getLatestPrice("   ")),
                () -> assertThrows(IllegalArgumentException.class, () -> service.getLatestPrice("btcUSDT")),
                () -> assertThrows(IllegalArgumentException.class, () -> service.getLatestPrice("BTC-USDT")),
                () -> assertThrows(IllegalArgumentException.class, () -> service.getLatestPrice("BTC_USDT"))
        );
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("принимает символ только из заглавных букв и цифр")
    void acceptsSymbolContainingOnlyUppercaseLettersAndDigits() {
        when(repository.findFirstBySymbolOrderByFetchedAtDescIdDesc("BTC123"))
                .thenReturn(Optional.empty());
        assertTrue(service.getLatestPrice("BTC123").isEmpty());
    }

    @Test
    @DisplayName("возвращает пустой период, если первая и последняя записи отсутствуют")
    void returnsEmptyPeriodWhenDataMissing() {
        when(repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtAscIdAsc("BTCUSDT", from, to))
                .thenReturn(Optional.empty());
        when(repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtDescIdDesc("BTCUSDT", from, to))
                .thenReturn(Optional.empty());
        assertTrue(service.getFirstAndLastPrices("BTCUSDT", from, to).isEmpty());
    }

    @Test
    @DisplayName("возвращает пустой период при отсутствии первой записи")
    void returnsEmptyPeriodWhenFirstRecordMissing() {
        when(repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtAscIdAsc("BTCUSDT", from, to))
                .thenReturn(Optional.empty());
        when(repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtDescIdDesc("BTCUSDT", from, to))
                .thenReturn(Optional.of(new PriceHistory("BTCUSDT", BigDecimal.TEN, to)));
        assertTrue(service.getFirstAndLastPrices("BTCUSDT", from, to).isEmpty());
    }

    @Test
    @DisplayName("возвращает пустой период при отсутствии последней записи")
    void returnsEmptyPeriodWhenLastRecordMissing() {
        when(repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtAscIdAsc("BTCUSDT", from, to))
                .thenReturn(Optional.of(new PriceHistory("BTCUSDT", BigDecimal.TEN, from)));
        when(repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtDescIdDesc("BTCUSDT", from, to))
                .thenReturn(Optional.empty());
        assertTrue(service.getFirstAndLastPrices("BTCUSDT", from, to).isEmpty());
    }

    @Test
    @DisplayName("отклоняет null from, to и pageable")
    void rejectsNullIntervalBoundsAndPageable() {
        var pageable = PageRequest.of(0, 10);
        assertAll(
                () -> assertThrows(NullPointerException.class,
                        () -> service.getPriceHistory("BTCUSDT", null, to, pageable)),
                () -> assertThrows(NullPointerException.class,
                        () -> service.getPriceHistory("BTCUSDT", from, null, pageable)),
                () -> assertThrows(NullPointerException.class,
                        () -> service.getPriceHistory("BTCUSDT", from, to, null)),
                () -> assertThrows(NullPointerException.class,
                        () -> service.getAveragePrice("BTCUSDT", null, to)),
                () -> assertThrows(NullPointerException.class,
                        () -> service.getAveragePrice("BTCUSDT", from, null)),
                () -> assertThrows(NullPointerException.class,
                        () -> service.getFirstAndLastPrices("BTCUSDT", null, to)),
                () -> assertThrows(NullPointerException.class,
                        () -> service.getFirstAndLastPrices("BTCUSDT", from, null))
        );
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("сохраняет положительную цену")
    void savesPrice() {
        PriceHistory saved = new PriceHistory("BTCUSDT", BigDecimal.TEN, from);
        when(repository.save(any(PriceHistory.class))).thenReturn(saved);
        assertSame(saved, service.savePrice("BTCUSDT", BigDecimal.TEN, from));
        verify(repository).save(any(PriceHistory.class));
    }

    @Test
    @DisplayName("отклоняет нулевую цену при сохранении")
    void rejectsZeroPriceOnSave() {
        assertThrows(IllegalArgumentException.class, () -> service.savePrice("BTCUSDT", BigDecimal.ZERO, from));
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("отклоняет отрицательную цену при сохранении")
    void rejectsNegativePriceOnSave() {
        assertThrows(IllegalArgumentException.class,
                () -> service.savePrice("BTCUSDT", new BigDecimal("-1"), from));
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("отклоняет null обязательные параметры при сохранении")
    void rejectsNullSaveParameters() {
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> service.savePrice(null, BigDecimal.ONE, from)),
                () -> assertThrows(NullPointerException.class, () -> service.savePrice("BTCUSDT", null, from)),
                () -> assertThrows(NullPointerException.class, () -> service.savePrice("BTCUSDT", BigDecimal.ONE, null))
        );
        verifyNoInteractions(repository);
    }
}
