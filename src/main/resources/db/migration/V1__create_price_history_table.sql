CREATE TABLE price_history (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    price NUMERIC(20, 8) NOT NULL,
    fetched_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_price_history_symbol_fetched_at
    ON price_history (symbol, fetched_at DESC);