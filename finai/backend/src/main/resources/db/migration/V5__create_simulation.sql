-- ============================================================
-- V5: Ambiente di simulazione investimenti (paper trading)
-- Crea wallet virtuale, posizioni simulate e storico operazioni.
-- ============================================================

-- ── sim_wallet ───────────────────────────────────────────────────────────────
-- Riga singola (id = 'default') che rappresenta la liquidità virtuale disponibile.
CREATE TABLE sim_wallet (
    id                VARCHAR(20)     PRIMARY KEY,
    cash_balance      NUMERIC(18, 4)  NOT NULL CHECK (cash_balance >= 0),
    starting_balance  NUMERIC(18, 4)  NOT NULL CHECK (starting_balance > 0),
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    reset_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

INSERT INTO sim_wallet (id, cash_balance, starting_balance)
VALUES ('default', 100000.0000, 100000.0000);

-- ── sim_positions ────────────────────────────────────────────────────────────
-- Posizioni aperte nel portafoglio simulato. avg_price = prezzo medio di carico
-- ponderato, aggiornato ad ogni acquisto.
CREATE TABLE sim_positions (
    id          VARCHAR(36)     PRIMARY KEY,
    ticker      VARCHAR(20)     NOT NULL UNIQUE,
    name        TEXT            NOT NULL,
    qty         NUMERIC(18, 6)  NOT NULL CHECK (qty > 0),
    avg_price   NUMERIC(18, 4)  NOT NULL CHECK (avg_price > 0),
    currency    VARCHAR(10)     NOT NULL DEFAULT 'USD',
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ── sim_trades ───────────────────────────────────────────────────────────────
-- Storico append-only di tutte le operazioni di acquisto/vendita simulate.
CREATE TABLE sim_trades (
    id           VARCHAR(36)     PRIMARY KEY,
    ticker       VARCHAR(20)     NOT NULL,
    name         TEXT            NOT NULL,
    side         VARCHAR(10)     NOT NULL CHECK (side IN ('BUY', 'SELL')),
    qty          NUMERIC(18, 6)  NOT NULL CHECK (qty > 0),
    price        NUMERIC(18, 4)  NOT NULL CHECK (price > 0),
    amount       NUMERIC(18, 4)  NOT NULL,
    realized_pnl NUMERIC(18, 4),
    currency     VARCHAR(10)     NOT NULL DEFAULT 'USD',
    executed_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sim_trades_executed ON sim_trades (executed_at DESC);
