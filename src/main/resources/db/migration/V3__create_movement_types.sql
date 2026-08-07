CREATE TABLE movement_types (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    kind TEXT NOT NULL CHECK (kind IN ('INCOME', 'EXPENSE')),
    code TEXT,
    active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S', 'now')),
    updated_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S', 'now'))
);

-- code identifies the fixed set of types the app's automatic flows (savings,
-- loans, loan payments, birthday gifts, settlements) rely on to record their
-- movements. Types with a code cannot be deactivated or deleted, only renamed;
-- types without one are free-form categories the administrator manages fully.
CREATE UNIQUE INDEX idx_movement_types_code ON movement_types(code) WHERE code IS NOT NULL;
CREATE INDEX idx_movement_types_kind_active ON movement_types(kind, active);
