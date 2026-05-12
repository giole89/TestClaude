-- ============================================================
-- V2: Indici per le query più frequenti
-- ============================================================

-- Portfolio: ricerca per ticker (refresh prezzi per simbolo)
CREATE INDEX idx_portfolio_ticker ON portfolio_items (ticker);

-- Alert: separazione active / history (fired_at IS NULL = attivo)
CREATE INDEX idx_alerts_fired ON alerts (fired_at) WHERE fired_at IS NULL;
CREATE INDEX idx_alerts_ticker ON alerts (ticker);

-- IPO watchlist: ordinamento per data prevista
CREATE INDEX idx_ipo_expected_date ON ipo_watchlist (expected_date);
