CREATE TABLE leagues (
    league_id              SERIAL PRIMARY KEY,
    name                   VARCHAR(100) NOT NULL,
    commissioner_user_id   INT REFERENCES users(user_id),
    max_teams              INT NOT NULL DEFAULT 10,
    roster_qb_slots        INT NOT NULL DEFAULT 1,
    roster_rb_slots        INT NOT NULL DEFAULT 2,
    roster_wr_slots        INT NOT NULL DEFAULT 2,
    salary_cap             NUMERIC(8,2) NOT NULL,
    regular_season_weeks   INT NOT NULL DEFAULT 4,
    entry_fee_coins        INT NOT NULL DEFAULT 400,
    draft_pick_seconds     INT NOT NULL DEFAULT 45,
    status                 VARCHAR(20) NOT NULL DEFAULT 'FORMING',
    current_week           INT NOT NULL DEFAULT 0,
    created_at             TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE league_teams (
    team_id        SERIAL PRIMARY KEY,
    league_id      INT NOT NULL REFERENCES leagues(league_id),
    user_id        INT NOT NULL REFERENCES users(user_id),
    team_name      VARCHAR(100) NOT NULL,
    wins           INT NOT NULL DEFAULT 0,
    losses         INT NOT NULL DEFAULT 0,
    points_for     NUMERIC(8,2) NOT NULL DEFAULT 0,
    points_against NUMERIC(8,2) NOT NULL DEFAULT 0,
    draft_position INT,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(league_id, user_id)
);

CREATE TABLE league_draft_picks (
    pick_id       SERIAL PRIMARY KEY,
    league_id     INT NOT NULL REFERENCES leagues(league_id),
    team_id       INT NOT NULL REFERENCES league_teams(team_id),
    player_id     INT NOT NULL REFERENCES players(player_id),
    round         INT NOT NULL,
    overall_pick  INT NOT NULL,
    position_slot VARCHAR(10) NOT NULL,
    picked_at     TIMESTAMP,
    UNIQUE(league_id, player_id),
    UNIQUE(league_id, overall_pick)
);

CREATE TABLE league_weekly_matchups (
    matchup_id     SERIAL PRIMARY KEY,
    league_id      INT NOT NULL REFERENCES leagues(league_id),
    week_number    INT NOT NULL,
    week_type      VARCHAR(20) NOT NULL,
    home_team_id   INT NOT NULL REFERENCES league_teams(team_id),
    away_team_id   INT NOT NULL REFERENCES league_teams(team_id),
    home_score     NUMERIC(8,2),
    away_score     NUMERIC(8,2),
    winner_team_id INT REFERENCES league_teams(team_id),
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);

CREATE TABLE league_matchup_game_logs (
    id             SERIAL PRIMARY KEY,
    matchup_id     INT NOT NULL REFERENCES league_weekly_matchups(matchup_id),
    team_id        INT NOT NULL REFERENCES league_teams(team_id),
    player_id      INT NOT NULL REFERENCES players(player_id),
    game_log_id    INT NOT NULL REFERENCES game_logs(game_log_id),
    fantasy_points NUMERIC(6,2) NOT NULL
);

CREATE INDEX idx_league_teams_league ON league_teams(league_id);
CREATE INDEX idx_league_draft_picks_league ON league_draft_picks(league_id);
CREATE INDEX idx_league_weekly_matchups_league_week ON league_weekly_matchups(league_id, week_number);
CREATE INDEX idx_league_matchup_game_logs_matchup ON league_matchup_game_logs(matchup_id);
