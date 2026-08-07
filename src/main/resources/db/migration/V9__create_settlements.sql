CREATE TABLE settlements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    person_id INTEGER NOT NULL REFERENCES persons(id),
    year INTEGER NOT NULL,
    savings_total_cents INTEGER NOT NULL,
    interest_rate_bps INTEGER NOT NULL,
    interest_amount_cents INTEGER NOT NULL,
    total_amount_cents INTEGER NOT NULL,
    prepared_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S', 'now')),
    UNIQUE (person_id, year)
);

-- A settlement is a snapshot calculated over that year's savings only (see
-- decision recorded in the project plan): preparing it again for the same
-- person/year replaces the row rather than accumulating duplicates.
CREATE INDEX idx_settlements_person ON settlements(person_id);
CREATE INDEX idx_settlements_year ON settlements(year);
