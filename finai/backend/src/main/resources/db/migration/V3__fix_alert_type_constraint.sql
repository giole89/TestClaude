-- ============================================================
-- V3: Allinea il check constraint alerts_type al formato
-- che @Enumerated(EnumType.STRING) scrive in DB (uppercase).
-- ============================================================

ALTER TABLE alerts DROP CONSTRAINT IF EXISTS alerts_type_check;

ALTER TABLE alerts ADD CONSTRAINT alerts_type_check
    CHECK (type IN ('ABOVE', 'BELOW', 'CHANGE_UP', 'CHANGE_DOWN'));
