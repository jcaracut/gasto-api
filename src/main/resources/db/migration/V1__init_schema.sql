CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name   VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE categories (
    id      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name    VARCHAR(100) NOT NULL UNIQUE,
    icon    VARCHAR(100) NOT NULL DEFAULT 'default'
);

CREATE TABLE expenses (
    id            UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID           NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id   UUID           NOT NULL REFERENCES categories(id),
    amount        NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    description   VARCHAR(500),
    expense_date  DATE           NOT NULL,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ
);

CREATE INDEX idx_expenses_user_id         ON expenses(user_id);
CREATE INDEX idx_expenses_user_date       ON expenses(user_id, expense_date);
CREATE INDEX idx_expenses_user_category   ON expenses(user_id, category_id);
CREATE INDEX idx_expenses_deleted_at      ON expenses(deleted_at) WHERE deleted_at IS NULL;
