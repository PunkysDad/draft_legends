#!/usr/bin/env python3
"""
Extract career game log data for 13 target NFL players from nfl-data-py.
Outputs players.csv and game_logs.csv into data/new_players/ for PostgreSQL migration.
"""

import os
import nfl_data_py as nfl
import pandas as pd

pd.set_option("display.max_rows", None)
pd.set_option("display.width", 120)

YEARS = list(range(1999, 2014))

TARGET_PLAYERS = {
    "Marshall Faulk": "RB",
    "Adrian Peterson": "RB",
    "Marvin Harrison": "WR",
    "Terrell Owens": "WR",
    "Larry Fitzgerald": "WR",
    "Andre Johnson": "WR",
    "Anquan Boldin": "WR",
    "Steve Smith": "WR",
    "Reggie Wayne": "WR",
    "Chad Johnson": "WR",
    "Calvin Johnson": "WR",
    "Isaac Bruce": "WR",
    "Hines Ward": "WR",
}

# Player IDs for disambiguation (multiple players share the same display name)
PLAYER_ID_OVERRIDES = {
    "Adrian Peterson": "00-0025394",  # Vikings "All Day" AD, drafted 2007 (not CHI RB 00-0021306)
}

STAT_COLS = [
    "carries", "rushing_yards", "rushing_tds",
    "receptions", "receiving_yards", "receiving_tds",
]

OUTPUT_DIR = "data/new_players"

print(f"Fetching weekly data for {YEARS[0]}–{YEARS[-1]}...")
df = nfl.import_weekly_data(YEARS)

for col in STAT_COLS:
    df[col] = df[col].fillna(0)

# Diagnostic: show all Adrian Peterson player_ids
print(f"\n--- Adrian Peterson disambiguation ---")
ap_rows = df[df["player_display_name"] == "Adrian Peterson"]
for pid, group in ap_rows.groupby("player_id"):
    seasons = sorted(group["season"].unique())
    teams = sorted(group["recent_team"].unique())
    print(f"  player_id={pid}, seasons={seasons[0]}–{seasons[-1]}, teams={teams}, games={len(group)}")
print(f"  → Keeping {PLAYER_ID_OVERRIDES['Adrian Peterson']} (MIN)")

# Match target players
print(f"\n--- Player matching ---")
matched = df[df["player_display_name"].isin(TARGET_PLAYERS.keys())]

# Apply player_id overrides for disambiguation
for name, required_id in PLAYER_ID_OVERRIDES.items():
    matched = matched[~((matched["player_display_name"] == name) & (matched["player_id"] != required_id))]
    print(f"  {name}: disambiguated — keeping player_id {required_id}")

# Steve Smith disambiguation: keep only the WR (CAR), not TE or other positions
steve_smith_rows = matched[matched["player_display_name"] == "Steve Smith"]
if len(steve_smith_rows) > 0:
    steve_smith_ids = steve_smith_rows.groupby("player_id").agg(
        games=("season", "count"),
        position=("position", "first"),
        teams=("recent_team", lambda x: ", ".join(sorted(x.unique())))
    )
    # The Panthers WR Steve Smith Sr. will have the most games and play for CAR
    car_ids = steve_smith_ids[steve_smith_ids["teams"].str.contains("CAR")].index
    if len(car_ids) > 0:
        keep_id = car_ids[0]
        drop_ids = [pid for pid in steve_smith_ids.index if pid != keep_id]
        if drop_ids:
            matched = matched[~((matched["player_display_name"] == "Steve Smith") & (matched["player_id"].isin(drop_ids)))]
            print(f"  Steve Smith: disambiguated — keeping player_id {keep_id} (CAR), dropped {len(drop_ids)} other(s)")

for name, pos in TARGET_PLAYERS.items():
    player_rows = matched[matched["player_display_name"] == name]
    if len(player_rows) > 0:
        seasons = sorted(player_rows["season"].unique())
        print(f"  ✓ {name} ({pos}): {len(player_rows)} games, seasons {seasons[0]}–{seasons[-1]}")
    else:
        print(f"  ✗ {name} ({pos}): NOT FOUND")


def calc_fantasy_points(row):
    pos = TARGET_PLAYERS[row["player_display_name"]]
    if pos == "RB":
        return (
            row["rushing_yards"] / 10
            + row["rushing_tds"] * 6
            + row["receptions"] * 0.5
            + row["receiving_yards"] / 10
            + row["receiving_tds"] * 6
        )
    else:  # WR
        return (
            row["receiving_yards"] / 10
            + row["receiving_tds"] * 6
            + row["receptions"] * 0.5
        )


matched = matched.copy()
matched["fantasy_points"] = matched.apply(calc_fantasy_points, axis=1)

# Build players.csv
players_rows = []
for name, pos in TARGET_PLAYERS.items():
    p = matched[matched["player_display_name"] == name]
    if len(p) == 0:
        continue
    parts = name.split(" ", 1)
    first_name = parts[0]
    last_name = parts[1] if len(parts) > 1 else ""
    avg_pts = round(p["fantasy_points"].mean(), 2)
    vol = round(p["fantasy_points"].std(), 2)
    salary = round(avg_pts, 2)
    seasons_played = p["season"].nunique()
    total_tds = int(p["rushing_tds"].sum() + p["receiving_tds"].sum())
    players_rows.append({
        "first_name": first_name,
        "last_name": last_name,
        "position": pos,
        "avg_fantasy_points": avg_pts,
        "volatility": vol,
        "salary": salary,
        "seasons_played": seasons_played,
        "total_touchdowns": total_tds,
    })

players_df = pd.DataFrame(players_rows)

# Build game_logs.csv
game_log_rows = []
for _, row in matched.iterrows():
    name = row["player_display_name"]
    pos = TARGET_PLAYERS[name]
    parts = name.split(" ", 1)
    first_name = parts[0]
    last_name = parts[1] if len(parts) > 1 else ""
    season = int(row["season"])
    week = int(row["week"])
    game_date = f"{season}-W{week}"

    if pos == "WR":
        rush_attempts = 0
        rush_yards = 0
        rush_tds = 0
    else:
        rush_attempts = int(row["carries"])
        rush_yards = round(float(row["rushing_yards"]), 1)
        rush_tds = int(row["rushing_tds"])

    game_log_rows.append({
        "first_name": first_name,
        "last_name": last_name,
        "position": pos,
        "season": season,
        "week": week,
        "game_date": game_date,
        "rush_attempts": rush_attempts,
        "rush_yards": rush_yards,
        "rush_tds": rush_tds,
        "receptions": int(row["receptions"]),
        "rec_yards": round(float(row["receiving_yards"]), 1),
        "rec_tds": int(row["receiving_tds"]),
        "fantasy_points": round(float(row["fantasy_points"]), 2),
    })

game_logs_df = pd.DataFrame(game_log_rows)

# Write CSVs
os.makedirs(OUTPUT_DIR, exist_ok=True)
players_csv = os.path.join(OUTPUT_DIR, "players.csv")
game_logs_csv = os.path.join(OUTPUT_DIR, "game_logs.csv")
players_df.to_csv(players_csv, index=False)
game_logs_df.to_csv(game_logs_csv, index=False)

print(f"\n--- Output ---")
print(f"  {players_csv}: {len(players_df)} players")
print(f"  {game_logs_csv}: {len(game_logs_df)} game logs")

# Summary table
print(f"\n{'=' * 75}")
print(f"  PLAYER SUMMARY")
print(f"{'=' * 75}")
summary = players_df[["first_name", "last_name", "position", "avg_fantasy_points", "volatility", "salary"]].copy()
summary.insert(0, "name", summary["first_name"] + " " + summary["last_name"])
summary = summary.drop(columns=["first_name", "last_name"])
games = game_logs_df.assign(name=game_logs_df["first_name"] + " " + game_logs_df["last_name"]).groupby("name").size().reset_index(name="games")
summary = summary.merge(games, on="name")
summary = summary[["name", "position", "games", "avg_fantasy_points", "volatility", "salary"]]
summary.columns = ["Name", "Pos", "Games", "Avg Pts", "Volatility", "Salary"]
summary = summary.sort_values(["Pos", "Avg Pts"], ascending=[True, False]).reset_index(drop=True)
summary.index += 1
summary.index.name = "Rank"
print(summary.to_string())
print()
