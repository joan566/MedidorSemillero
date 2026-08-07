CREATE TABLE savings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    person_id INTEGER NOT NULL REFERENCES persons(id),
    amount_cents INTEGER NOT NULL CHECK (amount_cents > 0),
    saving_date TEXT NOT NULL,
    payment_method TEXT NOT NULL CHECK (payment_method IN ('CASH', 'TRANSFER')),
    notes TEXT,
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S', 'now'))
);

CREATE INDEX idx_savings_person ON savings(person_id);
CREATE INDEX idx_savings_date ON savings(saving_date);
