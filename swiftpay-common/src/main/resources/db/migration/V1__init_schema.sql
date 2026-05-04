CREATE TABLE accounts (
    user_id       VARCHAR(64) PRIMARY KEY,
    balance_cents BIGINT      NOT NULL DEFAULT 0,
    currency      VARCHAR(3)  NOT NULL DEFAULT 'USD',
    version       BIGINT      NOT NULL DEFAULT 0
);

CREATE TABLE payments (
    id              BIGSERIAL PRIMARY KEY,
    transaction_id  VARCHAR(64) NOT NULL UNIQUE,
    sender_id       VARCHAR(64) NOT NULL,
    receiver_id     VARCHAR(64) NOT NULL,
    amount_cents    BIGINT      NOT NULL CHECK (amount_cents > 0),
    currency        VARCHAR(3)  NOT NULL,
    status          VARCHAR(32) NOT NULL,
    failure_reason  TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_sender ON payments (sender_id);
CREATE INDEX idx_payments_receiver ON payments (receiver_id);
CREATE INDEX idx_payments_created ON payments (created_at DESC);

CREATE TABLE payment_analytics (
    id              BIGSERIAL PRIMARY KEY,
    transaction_id  VARCHAR(64) NOT NULL,
    sender_id       VARCHAR(64) NOT NULL,
    receiver_id     VARCHAR(64) NOT NULL,
    amount_cents    BIGINT      NOT NULL,
    currency        VARCHAR(3)  NOT NULL,
    recorded_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_analytics_recorded ON payment_analytics (recorded_at DESC);
