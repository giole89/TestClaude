-- ============================================================
-- V1: Schema iniziale FINAI
-- Crea le tre tabelle persistenti: portfolio, alert, ipo_watchlist
-- ============================================================

-- ── portfolio_items ──────────────────────────────────────────────────────────
-- Posizioni nel portafoglio personale dell'utente.
-- load_price = prezzo medio di carico; current_price viene aggiornato via /refresh.
CREATE TABLE portfolio_items (
    id             VARCHAR(36)      PRIMARY KEY,
    ticker         VARCHAR(20)      NOT NULL,
    name           TEXT             NOT NULL,
    qty            NUMERIC(18, 6)   NOT NULL CHECK (qty > 0),
    load_price     NUMERIC(18, 4)   NOT NULL CHECK (load_price > 0),
    current_price  NUMERIC(18, 4),
    currency       VARCHAR(10)      NOT NULL DEFAULT 'USD',
    created_at     TIMESTAMPTZ      NOT NULL DEFAULT NOW()
);

-- ── alerts ───────────────────────────────────────────────────────────────────
-- Alert sui prezzi. fired_at / fired_price vengono popolati quando l'alert scatta
-- e lo spostano effettivamente in "history" (fired_at IS NOT NULL).
CREATE TABLE alerts (
    id           VARCHAR(36)     PRIMARY KEY,
    ticker       VARCHAR(20)     NOT NULL,
    type         VARCHAR(20)     NOT NULL
                     CHECK (type IN ('ABOVE', 'BELOW', 'CHANGE_UP', 'CHANGE_DOWN')),
    value        NUMERIC(18, 4)  NOT NULL CHECK (value > 0),
    fired_at     TIMESTAMPTZ,
    fired_price  NUMERIC(18, 4),
    created_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ── ipo_watchlist ─────────────────────────────────────────────────────────────
-- Watchlist personale di IPO da monitorare.
-- lockup_days: periodo di lock-up in giorni (default 180).
-- ipo_date: data effettiva di quotazione (NULL finché non quotata).
CREATE TABLE ipo_watchlist (
    id             VARCHAR(36)    PRIMARY KEY,
    ticker         VARCHAR(20),
    company_name   TEXT           NOT NULL,
    expected_date  DATE,
    exchange       VARCHAR(50),
    sector         VARCHAR(100),
    lockup_days    INTEGER        NOT NULL DEFAULT 180 CHECK (lockup_days > 0),
    ipo_price      NUMERIC(18, 4) CHECK (ipo_price > 0),
    ipo_date       DATE,
    notes          TEXT,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);
