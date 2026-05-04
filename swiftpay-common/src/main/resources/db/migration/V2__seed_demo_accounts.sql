INSERT INTO accounts (user_id, balance_cents, currency)
VALUES ('user-alice', 10000000, 'USD'),
       ('user-bob', 5000000, 'USD')
ON CONFLICT (user_id) DO NOTHING;
