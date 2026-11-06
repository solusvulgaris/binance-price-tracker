-- Binance closeTime was not stored before this migration and cannot be restored.
-- Existing local history is intentionally removed instead of assigning synthetic values.
TRUNCATE TABLE price_history;

ALTER TABLE price_history
    ADD COLUMN close_time BIGINT NOT NULL,
    ADD CONSTRAINT uk_price_history_symbol_close_time UNIQUE (symbol, close_time);
