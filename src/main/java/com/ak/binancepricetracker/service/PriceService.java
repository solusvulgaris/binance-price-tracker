package com.ak.binancepricetracker.service;

import com.ak.binancepricetracker.entity.PriceHistory;
import com.ak.binancepricetracker.repository.PriceHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
public class PriceService {

    private final PriceHistoryRepository repository;

    public PriceService(PriceHistoryRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    public Optional<PriceHistory> getLatestPrice(String symbol) {
        validateSymbol(symbol);
        return repository.findFirstBySymbolOrderByFetchedAtDescIdDesc(symbol);
    }

    public PriceHistory savePrice(String symbol, BigDecimal price, OffsetDateTime fetchedAt) {
        validateSymbol(symbol);
        Objects.requireNonNull(price, "price must not be null");
        Objects.requireNonNull(fetchedAt, "fetchedAt must not be null");
        if (price.signum() <= 0) {
            throw new IllegalArgumentException("price must be positive");
        }
        return repository.save(new PriceHistory(symbol, price, fetchedAt));
    }

    public Page<PriceHistory> getPriceHistory(
            String symbol, OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        validateSymbol(symbol);
        validateInterval(from, to);
        Objects.requireNonNull(pageable, "pageable must not be null");
        return repository.findBySymbolAndFetchedAtBetweenOrderByFetchedAtAscIdAsc(
                symbol, from, to, pageable);
    }

    public Optional<BigDecimal> getAveragePrice(
            String symbol, OffsetDateTime from, OffsetDateTime to) {
        validateSymbol(symbol);
        validateInterval(from, to);
        return repository.findAveragePriceBySymbolAndFetchedAtBetween(symbol, from, to);
    }

    public Optional<PricePeriod> getFirstAndLastPrices(
            String symbol, OffsetDateTime from, OffsetDateTime to) {
        validateSymbol(symbol);
        validateInterval(from, to);
        Optional<PriceHistory> first =
                repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtAscIdAsc(symbol, from, to);
        Optional<PriceHistory> last =
                repository.findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtDescIdDesc(symbol, from, to);
        if (first.isEmpty() || last.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PricePeriod(first.get(), last.get()));
    }

    private void validateInterval(OffsetDateTime from, OffsetDateTime to) {
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(to, "to must not be null");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
    }

    private void validateSymbol(String symbol) {
        Objects.requireNonNull(symbol, "symbol must not be null");
        if (symbol.isBlank() || !symbol.matches("[A-Z0-9]+")) {
            throw new IllegalArgumentException("symbol must contain only A-Z and 0-9 characters");
        }
    }

    public record PricePeriod(PriceHistory first, PriceHistory last) {
    }
}
