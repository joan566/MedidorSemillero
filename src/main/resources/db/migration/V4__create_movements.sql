CREATE TABLE movements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    movement_type_id INTEGER NOT NULL REFERENCES movement_types(id),
    fund TEXT NOT NULL CHECK (fund IN ('SAVINGS', 'BIRTHDAY')),
    payment_method TEXT NOT NULL CHECK (payment_method IN ('CASH', 'TRANSFER')),
    kind TEXT NOT NULL CHECK (kind IN ('INCOME', 'EXPENSE')),
    amount_cents INTEGER NOT NULL CHECK (amount_cents > 0),
    movement_date TEXT NOT NULL,
    person_id INTEGER REFERENCES persons(id),
    reference_table TEXT,
    reference_id INTEGER,
    notes TEXT,
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S', 'now'))
);

-- Fund balances (dashboard, per-fund totals) are always derived by summing
-- this table (INCOME minus EXPENSE, grouped by fund + payment_method) rather
-- than stored as a separate mutable counter, so a balance can never drift
-- out of sync with the movements that produced it.
CREATE INDEX idx_movements_date ON movements(movement_date);
CREATE INDEX idx_movements_fund_method_kind ON movements(fund, payment_method, kind);
CREATE INDEX idx_movements_person ON movements(person_id);
CREATE INDEX idx_movements_type ON movements(movement_type_id);
CREATE INDEX idx_movements_reference ON movements(reference_table, reference_id);
