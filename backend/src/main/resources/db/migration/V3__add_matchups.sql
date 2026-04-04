-- Reserved CPU user
INSERT INTO users (user_id, display_name, email, coin_balance)
VALUES (-1, 'CPU', 'cpu@legendsclash.internal', 0)
ON CONFLICT (user_id) DO NOTHING;

CREATE TABLE matchups (
    matchup_id           SERIAL PRIMARY KEY,
    mode                 VARCHAR(20) NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    user1_id             INT NOT NULL REFERENCES users(user_id),
    user2_id             INT REFERENCES users(user_id),
    is_vs_cpu            BOOLEAN NOT NULL DEFAULT FALSE,
    current_turn_user_id INT REFERENCES users(user_id),
    pick_deadline        TIMESTAMP,
    user1_score          NUMERIC(6,2),
    user2_score          NUMERIC(6,2),
    winner_user_id       INT REFERENCES users(user_id),
    created_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at         TIMESTAMP
);

CREATE TABLE draft_picks (
    pick_id     SERIAL PRIMARY KEY,
    matchup_id  INT NOT NULL REFERENCES matchups(matchup_id),
    user_id     INT NOT NULL REFERENCES users(user_id),
    player_id   INT NOT NULL REFERENCES players(player_id),
    pick_number INT NOT NULL,
    is_cpu_pick BOOLEAN NOT NULL DEFAULT FALSE,
    picked_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE matchup_game_logs (
    id             SERIAL PRIMARY KEY,
    matchup_id     INT NOT NULL REFERENCES matchups(matchup_id),
    user_id        INT NOT NULL REFERENCES users(user_id),
    player_id      INT NOT NULL REFERENCES players(player_id),
    game_log_id    INT NOT NULL REFERENCES game_logs(game_log_id),
    fantasy_points NUMERIC(6,2) NOT NULL
);
