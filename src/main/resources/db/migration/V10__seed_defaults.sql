INSERT INTO app_settings (key, value) VALUES
    ('loan.interest_rate_bps', '300'),
    ('settlement.interest_rate_bps', '300'),
    ('birthday.gift_default_amount_cents', '20000000');

INSERT INTO movement_types (name, kind, code) VALUES
    ('Ahorro', 'INCOME', 'SAVINGS_CONTRIBUTION'),
    ('Pago de préstamo', 'INCOME', 'LOAN_PAYMENT'),
    ('Otro ingreso', 'INCOME', NULL),
    ('Préstamo', 'EXPENSE', 'LOAN_DISBURSEMENT'),
    ('Regalo de cumpleaños', 'EXPENSE', 'BIRTHDAY_GIFT'),
    ('Liquidación', 'EXPENSE', 'SETTLEMENT_PAYOUT'),
    ('Otro egreso', 'EXPENSE', NULL);
