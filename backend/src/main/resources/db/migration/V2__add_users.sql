CREATE TABLE users (
    user_id       SERIAL PRIMARY KEY,
    google_uid    VARCHAR(128) UNIQUE,
    apple_uid     VARCHAR(128) UNIQUE,
    display_name  VARCHAR(100),
    email         VARCHAR(255),
    coin_balance  INT NOT NULL DEFAULT 500,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    last_login_at TIMESTAMP
);
