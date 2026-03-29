CREATE TABLE players (
    player_id          SERIAL PRIMARY KEY,
    first_name         VARCHAR(50) NOT NULL,
    last_name          VARCHAR(50) NOT NULL,
    position           VARCHAR(5)  NOT NULL,
    photo_url          TEXT,
    seasons_played     INT,
    total_touchdowns   INT,
    total_interceptions INT,
    salary             NUMERIC(6,2),
    volatility         NUMERIC(6,2)
);

CREATE TABLE player_seasons (
    season_id   SERIAL PRIMARY KEY,
    player_id   INT NOT NULL REFERENCES players(player_id),
    season      INT NOT NULL,
    games_played INT
);

CREATE TABLE game_logs (
    game_log_id          SERIAL PRIMARY KEY,
    source_doc_id        VARCHAR(128),
    player_id            INT NOT NULL REFERENCES players(player_id),
    season               INT,
    week                 INT,
    game_date            DATE,
    position             VARCHAR(5) NOT NULL,
    pass_attempts        INT,
    pass_completions     INT,
    completion_pct       NUMERIC(5,2),
    yards_per_attempt    NUMERIC(5,2),
    pass_yards           NUMERIC(6,1),
    pass_tds             INT,
    interceptions        INT,
    passer_rating        NUMERIC(5,1),
    sacks                INT,
    rush_attempts        INT,
    rush_yards           NUMERIC(6,1),
    yards_per_carry      NUMERIC(5,2),
    rush_long            INT,
    rush_tds             INT,
    receptions           INT,
    rec_yards            NUMERIC(6,1),
    yards_per_reception  NUMERIC(5,2),
    rec_long             INT,
    rec_tds              INT,
    wr_receptions        INT,
    wr_yards             NUMERIC(6,1),
    wr_tds               INT,
    yards_per_wr_reception NUMERIC(5,2),
    fantasy_points       NUMERIC(6,2)
);

-- Indexes for game log random pull queries
CREATE INDEX idx_game_logs_player_id ON game_logs(player_id);
CREATE INDEX idx_game_logs_position ON game_logs(position);

-- Prevent duplicate seasons per player
ALTER TABLE player_seasons
    ADD CONSTRAINT uq_player_season UNIQUE (player_id, season);

ALTER TABLE game_logs
    ADD CONSTRAINT uq_game_log UNIQUE (player_id, source_doc_id);
