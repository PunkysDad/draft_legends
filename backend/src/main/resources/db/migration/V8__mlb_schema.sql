CREATE TABLE mlb_players (
    player_id        SERIAL PRIMARY KEY,
    first_name       VARCHAR(50)    NOT NULL,
    last_name        VARCHAR(50)    NOT NULL,
    player_type      VARCHAR(10)    NOT NULL,  -- 'HITTER' or 'PITCHER'
    photo_url        TEXT           NOT NULL DEFAULT '',
    salary           NUMERIC(6,2)   NOT NULL,
    volatility       NUMERIC(6,2)   NOT NULL,
    avg_fantasy_points NUMERIC(6,2) NOT NULL,
    seasons_played   INT            NOT NULL,
    total_home_runs  INT,           -- hitters only, nullable
    total_strikeouts INT,           -- pitchers only, nullable
    key_mlbam        INT            NOT NULL UNIQUE,
    CONSTRAINT uq_mlb_player UNIQUE (first_name, last_name)
);

CREATE TABLE mlb_game_logs (
    game_log_id          SERIAL PRIMARY KEY,
    player_id            INT            NOT NULL REFERENCES mlb_players(player_id),
    season               INT            NOT NULL,
    game_date            DATE           NOT NULL,
    opponent             VARCHAR(100),
    is_home              BOOLEAN,
    -- hitter fields (null for pitchers)
    at_bats              INT,
    hits                 INT,
    singles              INT,
    doubles              INT,
    triples              INT,
    home_runs            INT,
    rbi                  INT,
    runs                 INT,
    stolen_bases         INT,
    walks                INT,
    strikeouts_batting   INT,
    -- pitcher fields (null for hitters)
    innings_pitched      NUMERIC(5,2),
    pitcher_strikeouts   INT,
    walks_allowed        INT,
    earned_runs          INT,
    hits_allowed         INT,
    wins                 INT,
    losses               INT,
    saves                INT,
    -- shared
    fantasy_points       NUMERIC(6,2)   NOT NULL,
    source_doc_id        VARCHAR(100)   NOT NULL,
    CONSTRAINT uq_mlb_game_log UNIQUE (source_doc_id)
);

CREATE INDEX idx_mlb_game_logs_player_id ON mlb_game_logs(player_id);
CREATE INDEX idx_mlb_game_logs_season ON mlb_game_logs(season);
