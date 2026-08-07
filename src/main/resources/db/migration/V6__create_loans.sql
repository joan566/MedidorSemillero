CREATE TABLE loans (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    person_id INTEGER NOT NULL REFERENCES persons(id),
    principal_amount_cents INTEGER NOT NULL CHECK (principal_amount_cents > 0),
    interest_rate_bps INTEGER NOT NULL,
    interest_amount_cents INTEGER NOT NULL,
    total_amount_cents INTEGER NOT NULL,
    paid_amount_cents INTEGER NOT NULL DEFAULT 0,
    outstanding_amount_cents INTEGER NOT NULL,
    loan_date TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'PAID')),
    notes TEXT,
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S', 'now')),
    updated_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S', 'now'))
);

-- interest_rate_bps stores the rate applied to this specific loan, in basis
-- points (300 = 3.00%), as a snapshot at loan creation time. Changing the
-- default rate in app_settings later never rewrites existing loans.
CREATE INDEX idx_loans_person ON loans(person_id);
CREATE INDEX idx_loans_status ON loans(status);
