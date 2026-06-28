-- Modulo di finanza personale: import estratti conto, spese fisse,
-- budget mensile e profilo investitore (questionario).

CREATE TABLE bank_transactions (
    id            VARCHAR(36) PRIMARY KEY,
    tx_date       DATE NOT NULL,
    description   VARCHAR(500) NOT NULL,
    amount        NUMERIC(14,2) NOT NULL,      -- negativo = uscita, positivo = entrata
    category      VARCHAR(50) NOT NULL,
    type          VARCHAR(20) NOT NULL CHECK (type IN ('INCOME', 'VARIABLE_EXPENSE')),
    source_file   VARCHAR(255),
    imported_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_bank_tx_date ON bank_transactions (tx_date);
CREATE INDEX idx_bank_tx_type ON bank_transactions (type);

CREATE TABLE fixed_expenses (
    id          VARCHAR(36) PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    category    VARCHAR(50) NOT NULL,
    amount      NUMERIC(14,2) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE investor_profile (
    id              VARCHAR(20) PRIMARY KEY DEFAULT 'default',
    goal            VARCHAR(50),
    goal_note       VARCHAR(255),
    horizon         VARCHAR(30),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO investor_profile (id) VALUES ('default');
