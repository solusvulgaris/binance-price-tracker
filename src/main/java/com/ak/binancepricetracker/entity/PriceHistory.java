package com.ak.binancepricetracker.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "price_history")
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal price;

    @Column(name = "fetched_at", nullable = false)
    private OffsetDateTime fetchedAt;

    protected PriceHistory() {
        // Required by JPA
    }

    public PriceHistory(String symbol, BigDecimal price, OffsetDateTime fetchedAt) {
        this.symbol = symbol;
        this.price = price;
        this.fetchedAt = fetchedAt;
    }

    public Long getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public OffsetDateTime getFetchedAt() {
        return fetchedAt;
    }
}