-- Moves loan interest from "3% once on the original principal" to "3% every
-- month on the outstanding principal balance". principal_balance_cents now
-- tracks what's still owed in capital, interest_owed_cents tracks accrued,
-- unpaid interest (mora — never capitalized into principal_balance_cents),
-- and next_accrual_date marks when the next monthly charge is due.
CREATE TABLE loan_interest_charges (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    loan_id INTEGER NOT NULL REFERENCES loans(id),
    due_date TEXT NOT NULL,
    principal_balance_cents INTEGER NOT NULL,
    interest_amount_cents INTEGER NOT NULL,
    paid_amount_cents INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'PARTIAL', 'PAID')),
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S', 'now'))
);

CREATE INDEX idx_loan_interest_charges_loan ON loan_interest_charges(loan_id);
CREATE INDEX idx_loan_interest_charges_status ON loan_interest_charges(loan_id, status);

ALTER TABLE loans ADD COLUMN principal_balance_cents INTEGER NOT NULL DEFAULT 0;
ALTER TABLE loans ADD COLUMN interest_owed_cents INTEGER NOT NULL DEFAULT 0;
ALTER TABLE loans ADD COLUMN next_accrual_date TEXT NOT NULL DEFAULT '1970-01-01';

-- Backfill: there is no real production data behind this table yet, so
-- existing rows are simply treated as pure principal with nothing accrued,
-- with the first monthly charge due one month after they were created.
UPDATE loans
SET principal_balance_cents = outstanding_amount_cents,
    next_accrual_date = date(loan_date, '+1 month');

ALTER TABLE loans DROP COLUMN interest_amount_cents;
ALTER TABLE loans DROP COLUMN total_amount_cents;
ALTER TABLE loans DROP COLUMN outstanding_amount_cents;

-- Records how each historical payment split between interest and principal.
ALTER TABLE loan_payments ADD COLUMN interest_portion_cents INTEGER NOT NULL DEFAULT 0;
ALTER TABLE loan_payments ADD COLUMN principal_portion_cents INTEGER NOT NULL DEFAULT 0;

UPDATE loan_payments SET principal_portion_cents = amount_cents;
