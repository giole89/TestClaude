-- ============================================================
-- V4: Tabella watchlist_items
-- Permette all'utente di monitorare ticker con target price opzionale
-- ============================================================

CREATE TABLE watchlist_items (
    id           VARCHAR(50)     PRIMARY KEY,
    ticker       VARCHAR(20)     NOT NULL UNIQUE,
    name         VARCHAR(255),
    target_price DECIMAL(12, 4),
    note         TEXT,
    created_at   BIGINT          NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000
);

CREATE INDEX idx_watchlist_ticker ON watchlist_items (ticker);
CREATE INDEX idx_watchlist_created ON watchlist_items (created_at DESC);
