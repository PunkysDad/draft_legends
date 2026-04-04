#!/usr/bin/env python3
"""
Extract full career game log data for 35 MLB legends from the MLB Stats API.
Outputs mlb_players.csv and mlb_game_logs.csv into data/mlb_players/.
"""

import subprocess
import sys
import importlib
import json
import os
import time
import urllib.request
import statistics

os.environ["PYTHONUNBUFFERED"] = "1"

for package, import_name in [("MLB-StatsAPI", "statsapi"), ("pybaseball", "pybaseball")]:
    try:
        importlib.import_module(import_name)
    except ImportError:
        print(f"Installing {package}...")
        subprocess.check_call([sys.executable, "-m", "pip", "install", package, "--quiet"])
        importlib.invalidate_caches()

from pybaseball import playerid_lookup

print("Loading pybaseball player lookup table...", flush=True)
_warmup = playerid_lookup("bonds", "barry")
print("  Lookup table ready.", flush=True)

# -----------------------------------------------------------------
# Target players
# -----------------------------------------------------------------

HITTERS = [
    ("Willie", "Mays"),
    ("Mickey", "Mantle"),
    ("Hank", "Aaron"),
    ("Babe", "Ruth"),
    ("Ted", "Williams"),
    ("Barry", "Bonds"),
    ("Ken", "Griffey Jr.", 115135),
    ("Ken", "Griffey Sr.", 115136),
    ("Alex", "Rodriguez"),
    ("Derek", "Jeter"),
    ("Mike", "Piazza"),
    ("Frank", "Thomas"),
    ("Reggie", "Jackson"),
    ("Roberto", "Clemente"),
    ("Johnny", "Bench"),
    ("Chipper", "Jones"),
    ("Albert", "Pujols"),
    ("Manny", "Ramirez"),
    ("Jim", "Thome"),
    ("George", "Brett"),
]

PITCHERS = [
    ("Roger", "Clemens"),
    ("Greg", "Maddux"),
    ("Pedro", "Martinez", 118377),
    ("Nolan", "Ryan"),
    ("Sandy", "Koufax"),
    ("Tom", "Seaver"),
    ("Bob", "Gibson"),
    ("Cy", "Young"),
    ("Walter", "Johnson"),
    ("Curt", "Schilling"),
    ("John", "Smoltz"),
    ("Roy", "Halladay"),
    ("David", "Cone"),
    ("Dwight", "Gooden"),
    ("Randy", "Johnson", 116615),
]

# Overrides for pybaseball disambiguation (keyed by lowercase last, first)
PLAYER_ID_OVERRIDES = {
    ("griffey", "ken", 115135): 115135,
    ("griffey sr", "ken", 115136): 115136,
    ("martinez", "pedro"): 118377,
    ("johnson", "randy"): 116615,
}

OUTPUT_DIR = "data/mlb_players"

# -----------------------------------------------------------------
# Helpers
# -----------------------------------------------------------------

def lookup_mlbam(first, last):
    """Look up key_mlbam via pybaseball, checking overrides first."""
    clean_last = last.replace(" Jr.", "").replace(" Sr.", "").strip()
    key2 = (clean_last.lower(), first.lower())

    # Check 2-tuple overrides
    if key2 in PLAYER_ID_OVERRIDES:
        return PLAYER_ID_OVERRIDES[key2]

    try:
        df = playerid_lookup(clean_last, first)
        if df.empty:
            return None
        return int(df.iloc[0]["key_mlbam"])
    except Exception as e:
        print(f"    Lookup error: {e}", flush=True)
        return None


def get_career_years(player_id):
    """Get debut and last played years from the MLB API."""
    url = f"https://statsapi.mlb.com/api/v1/people/{player_id}"
    try:
        with urllib.request.urlopen(url, timeout=15) as resp:
            data = json.loads(resp.read().decode())
        person = data["people"][0]
        debut = person.get("mlbDebutDate", "")
        last = person.get("lastPlayedDate", "")
        if debut and last:
            return list(range(int(debut[:4]), int(last[:4]) + 1))
    except Exception:
        pass
    return []


def fetch_game_logs(player_id, season, group):
    """Fetch game logs for one season from the MLB Stats API."""
    url = (
        f"https://statsapi.mlb.com/api/v1/people/{player_id}/stats"
        f"?stats=gameLog&group={group}&season={season}"
    )
    try:
        with urllib.request.urlopen(url, timeout=15) as resp:
            data = json.loads(resp.read().decode())
        stats_list = data.get("stats", [])
        if stats_list and stats_list[0].get("splits"):
            return stats_list[0]["splits"]
    except Exception:
        pass
    return []


def parse_innings_pitched(ip_raw):
    """Convert MLB IP string (e.g. '6.1' = 6⅓) to true float."""
    ip_str = str(ip_raw)
    if "." in ip_str:
        parts = ip_str.split(".")
        return int(parts[0]) + int(parts[1]) / 3.0
    return float(ip_str)


def hitter_fantasy_points(stat):
    """ESPN standard hitter scoring."""
    hits = stat.get("hits", 0)
    doubles = stat.get("doubles", 0)
    triples = stat.get("triples", 0)
    home_runs = stat.get("homeRuns", 0)
    singles = hits - doubles - triples - home_runs
    rbi = stat.get("rbi", 0)
    runs = stat.get("runs", 0)
    sb = stat.get("stolenBases", 0)
    bb = stat.get("baseOnBalls", 0)
    k = stat.get("strikeOuts", 0)
    return singles + doubles * 2 + triples * 3 + home_runs * 4 + rbi + runs + sb + bb + k * (-1)


def pitcher_fantasy_points(stat):
    """ESPN standard pitcher scoring."""
    ip = parse_innings_pitched(stat.get("inningsPitched", "0"))
    k = stat.get("strikeOuts", 0)
    bb = stat.get("baseOnBalls", 0)
    er = stat.get("earnedRuns", 0)
    h = stat.get("hits", 0)
    w = stat.get("wins", 0)
    losses = stat.get("losses", 0)
    sv = stat.get("saves", 0)
    return ip * 3 + k - bb + er * (-2) + h * (-1) + w * 2 + losses * (-2) + sv * 5


# -----------------------------------------------------------------
# Processing
# -----------------------------------------------------------------

def process_players(player_list, player_type, group, scoring_fn):
    """Process a list of players, return (player_summaries, game_log_rows)."""
    summaries = []
    game_logs = []

    for entry in player_list:
        first = entry[0]
        last = entry[1]
        forced_id = entry[2] if len(entry) > 2 else None
        display_name = f"{first} {last}"

        print(f"  {display_name}...", end=" ", flush=True)

        # Resolve player ID
        if forced_id:
            player_id = forced_id
        else:
            player_id = lookup_mlbam(first, last)

        if player_id is None:
            print("NOT FOUND", flush=True)
            continue

        time.sleep(1)

        years = get_career_years(player_id)
        if not years:
            print("no career years", flush=True)
            continue

        time.sleep(1)

        all_points = []
        seasons_with_data = 0
        total_hr = 0
        total_k = 0

        for year in years:
            logs = fetch_game_logs(player_id, year, group)
            if logs:
                seasons_with_data += 1
                for split in logs:
                    stat = split.get("stat", {})
                    pts = scoring_fn(stat)
                    all_points.append(pts)

                    opponent = split.get("opponent", {}).get("name", "")
                    game_date = split.get("date", "")
                    is_home = split.get("isHome", False)

                    if player_type == "HITTER":
                        hits = stat.get("hits", 0)
                        doubles = stat.get("doubles", 0)
                        triples = stat.get("triples", 0)
                        home_runs = stat.get("homeRuns", 0)
                        singles = hits - doubles - triples - home_runs
                        total_hr += home_runs

                        game_logs.append({
                            "first_name": first,
                            "last_name": last,
                            "player_type": player_type,
                            "season": year,
                            "game_date": game_date,
                            "opponent": opponent,
                            "is_home": is_home,
                            "at_bats": stat.get("atBats", 0),
                            "hits": hits,
                            "singles": singles,
                            "doubles": doubles,
                            "triples": triples,
                            "home_runs": home_runs,
                            "rbi": stat.get("rbi", 0),
                            "runs": stat.get("runs", 0),
                            "stolen_bases": stat.get("stolenBases", 0),
                            "walks": stat.get("baseOnBalls", 0),
                            "strikeouts_batting": stat.get("strikeOuts", 0),
                            "innings_pitched": 0,
                            "pitcher_strikeouts": 0,
                            "walks_allowed": 0,
                            "earned_runs": 0,
                            "hits_allowed": 0,
                            "wins": 0,
                            "losses": 0,
                            "saves": 0,
                            "fantasy_points": round(pts, 2),
                        })
                    else:
                        k = stat.get("strikeOuts", 0)
                        total_k += k
                        ip = parse_innings_pitched(stat.get("inningsPitched", "0"))

                        game_logs.append({
                            "first_name": first,
                            "last_name": last,
                            "player_type": player_type,
                            "season": year,
                            "game_date": game_date,
                            "opponent": opponent,
                            "is_home": is_home,
                            "at_bats": 0,
                            "hits": 0,
                            "singles": 0,
                            "doubles": 0,
                            "triples": 0,
                            "home_runs": 0,
                            "rbi": 0,
                            "runs": 0,
                            "stolen_bases": 0,
                            "walks": 0,
                            "strikeouts_batting": 0,
                            "innings_pitched": round(ip, 2),
                            "pitcher_strikeouts": k,
                            "walks_allowed": stat.get("baseOnBalls", 0),
                            "earned_runs": stat.get("earnedRuns", 0),
                            "hits_allowed": stat.get("hits", 0),
                            "wins": stat.get("wins", 0),
                            "losses": stat.get("losses", 0),
                            "saves": stat.get("saves", 0),
                            "fantasy_points": round(pts, 2),
                        })

            time.sleep(1)

        if not all_points:
            print("no game data", flush=True)
            continue

        avg_pts = round(statistics.mean(all_points), 2)
        vol = round(statistics.stdev(all_points), 2) if len(all_points) > 1 else 0.0

        summaries.append({
            "first_name": first,
            "last_name": last,
            "player_type": player_type,
            "avg_fantasy_points": avg_pts,
            "volatility": vol,
            "salary": round(avg_pts, 2),
            "seasons_played": seasons_with_data,
            "total_home_runs": total_hr if player_type == "HITTER" else 0,
            "total_strikeouts": total_k if player_type == "PITCHER" else 0,
            "key_mlbam": player_id,
        })

        print(
            f"{len(all_points)} games, {seasons_with_data} seasons, "
            f"avg={avg_pts}, vol={vol}",
            flush=True,
        )

    return summaries, game_logs


# -----------------------------------------------------------------
# Run
# -----------------------------------------------------------------

print("\n" + "=" * 85)
print("  MLB Legends Extraction")
print("=" * 85)

print(f"\n--- Processing {len(HITTERS)} hitters ---", flush=True)
hitter_summaries, hitter_logs = process_players(HITTERS, "HITTER", "hitting", hitter_fantasy_points)

print(f"\n--- Processing {len(PITCHERS)} pitchers ---", flush=True)
pitcher_summaries, pitcher_logs = process_players(PITCHERS, "PITCHER", "pitching", pitcher_fantasy_points)

all_summaries = hitter_summaries + pitcher_summaries
all_logs = hitter_logs + pitcher_logs

# Write CSVs
os.makedirs(OUTPUT_DIR, exist_ok=True)

players_csv = os.path.join(OUTPUT_DIR, "mlb_players.csv")
logs_csv = os.path.join(OUTPUT_DIR, "mlb_game_logs.csv")

player_cols = [
    "first_name", "last_name", "player_type", "avg_fantasy_points",
    "volatility", "salary", "seasons_played", "total_home_runs",
    "total_strikeouts", "key_mlbam",
]

log_cols = [
    "first_name", "last_name", "player_type", "season", "game_date", "opponent", "is_home",
    "at_bats", "hits", "singles", "doubles", "triples", "home_runs", "rbi", "runs",
    "stolen_bases", "walks", "strikeouts_batting",
    "innings_pitched", "pitcher_strikeouts", "walks_allowed", "earned_runs",
    "hits_allowed", "wins", "losses", "saves",
    "fantasy_points",
]

with open(players_csv, "w") as f:
    f.write(",".join(player_cols) + "\n")
    for s in all_summaries:
        f.write(",".join(str(s[c]) for c in player_cols) + "\n")

with open(logs_csv, "w") as f:
    f.write(",".join(log_cols) + "\n")
    for row in all_logs:
        values = []
        for c in log_cols:
            v = row[c]
            # Quote strings that may contain commas
            if isinstance(v, str) and "," in v:
                values.append(f'"{v}"')
            else:
                values.append(str(v))
        f.write(",".join(values) + "\n")

print(f"\n--- Output ---")
print(f"  {players_csv}: {len(all_summaries)} players")
print(f"  {logs_csv}: {len(all_logs)} game logs")

# Summary table
print(f"\n{'=' * 90}")
print(f"  PLAYER SUMMARY")
print(f"{'=' * 90}")
print(
    f"{'Rank':>4}  {'Name':<22} {'Type':<8} {'Games':>6} "
    f"{'Avg Pts':>8} {'Volatility':>10} {'Salary':>8} {'Seasons':>8}"
)
print(
    f"{'----':>4}  {'----':<22} {'----':<8} {'-----':>6} "
    f"{'-------':>8} {'----------':>10} {'------':>8} {'-------':>8}"
)
all_summaries.sort(key=lambda x: (x["player_type"], -x["avg_fantasy_points"]))
for i, s in enumerate(all_summaries, 1):
    games = sum(1 for r in all_logs if r["first_name"] == s["first_name"] and r["last_name"] == s["last_name"])
    print(
        f"{i:4d}  {s['first_name'] + ' ' + s['last_name']:<22} {s['player_type']:<8} {games:6d} "
        f"{s['avg_fantasy_points']:8.2f} {s['volatility']:10.2f} {s['salary']:8.2f} {s['seasons_played']:8d}"
    )
print()
