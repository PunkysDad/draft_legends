CREATE TABLE wallets (
    wallet_id  SERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL UNIQUE,
    balance    INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE coin_transactions (
    transaction_id   SERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    amount           INT NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    description      VARCHAR(255),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES wallets(user_id)
);

CREATE INDEX idx_coin_transactions_user_id ON coin_transactions(user_id);
