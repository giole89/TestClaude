-- Aggiunge i dati necessari per i controlli pre-investimento: fondo di
-- emergenza (liquidità già accantonata dall'utente) e debiti ad alto
-- interesse tra le spese fisse (es. finanziamenti, prestiti, carte revolving).

ALTER TABLE investor_profile ADD COLUMN liquid_savings NUMERIC(14,2);

ALTER TABLE fixed_expenses ADD COLUMN interest_rate_pct NUMERIC(5,2);
