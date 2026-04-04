#!/usr/bin/env python3
"""
Explore legendary MLB hitter and pitcher candidates from the MLB Stats API.
Computes fantasy points using ESPN standard scoring and prints browsable tables.
"""

import subprocess
import sys
import importlib
import json
import time
import urllib.request

for package, import_name in [("MLB-StatsAPI", "statsapi"), ("pybaseball", "pybaseball")]:
    try:
        importlib.import_module(import_name)
    except ImportError:
        print(f"Installing {package}...")
        subprocess.check_call([sys.executable, "-m", "pip", "install", package, "--quiet"])
        importlib.invalidate_caches()

import os
os.environ["PYTHONUNBUFFERED"] = "1"

from pybaseball import playerid_lookup
# Pre-cache the lookup table
print("Loading pybaseball player lookup table...", flush=True)
_warmup = playerid_lookup("bonds", "barry")
print("  Lookup table ready.", flush=True)

# -----------------------------------------------------------------
# Candidate players
# -----------------------------------------------------------------
HITTERS = [
    "Willie Mays", "Mickey Mantle", "Hank Aaron", "Babe Ruth", "Ted Williams",
    "Barry Bonds", "Ken Griffey Jr.", "Alex Rodriguez", "Derek Jeter", "Mike Piazza",
    "Frank Thomas", "Reggie Jackson", "Roberto Clemente", "Johnny Bench",
    "Chipper Jones", "Albert Pujols", "Manny Ramirez", "Jim Thome",
    "George Brett", "Cal Ripken Jr.",
]

PITCHERS = [
    "Roger Clemens", "Randy Johnson", "Greg Maddux", "Pedro Martinez", "Nolan Ryan",
    "Sandy Koufax", "Tom Seaver", "Bob Gibson", "Cy Young", "Walter Johnson",
    "Curt Schilling", "John Smoltz", "Roy Halladay", "David Cone", "Dwight Gooden",
]

# Hardcoded overrides for players where pybaseball returns the wrong match
PLAYER_ID_OVERRIDES = {
    ('griffey', 'ken'): 115135,       # Ken Griffey Jr. (1989–2010), not Sr.
    ('ripken', 'cal'): 121222,        # Cal Ripken Jr. (1981–2001)
    ('johnson', 'randy'): 116615,     # Randy Johnson "Big Unit" (1988–2009)
    ('martinez', 'pedro'): 118377,    # Pedro Martinez (1992–2009)
}

# -----------------------------------------------------------------
# Helpers
# -----------------------------------------------------------------

def lookup_mlbam(last, first):
    """Look up a player's key_mlbam via pybaseball."""
    try:
        df = playerid_lookup(last, first)
        if df.empty:
            return None
        return int(df.iloc[0]["key_mlbam"])
    except Exception as e:
        print(f"    Lookup error: {e}")
        return None


def fetch_game_logs(player_id: int, season: int, group: str) -> list:
    """Fetch game logs for a single season from the MLB Stats API."""
    url = (
        f"https://statsapi.mlb.com/api/v1/people/{player_id}/stats"
        f"?stats=gameLog&group={group}&season={season}"
    )
    try:
        with urllib.request.urlopen(url, timeout=15) as resp:
            data = json.loads(resp.read().decode())
        stats = data.get("stats", [])
        if stats and stats[0].get("splits"):
            return stats[0]["splits"]
    except Exception:
        pass
    return []


def parse_innings_pitched(ip_raw) -> float:
    """Convert MLB IP string (e.g. '6.1' = 6⅓ innings) to a true float."""
    ip_str = str(ip_raw)
    if "." in ip_str:
        parts = ip_str.split(".")
        return int(parts[0]) + int(parts[1]) / 3.0
    return float(ip_str)


def hitter_fantasy_points(stat: dict) -> float:
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
    return singles * 1 + doubles * 2 + triples * 3 + home_runs * 4 + rbi + runs + sb + bb + k * (-1)


def pitcher_fantasy_points(stat: dict) -> float:
    """ESPN standard pitcher scoring."""
    ip = parse_innings_pitched(stat.get("inningsPitched", "0"))
    k = stat.get("strikeOuts", 0)
    bb = stat.get("baseOnBalls", 0)
    er = stat.get("earnedRuns", 0)
    h = stat.get("hits", 0)
    w = stat.get("wins", 0)
    l = stat.get("losses", 0)
    sv = stat.get("saves", 0)
    return ip * 3 + k * 1 + bb * (-1) + er * (-2) + h * (-1) + w * 2 + l * (-2) + sv * 5


def get_career_years(player_id):
    """Get all season years a player appeared in from the MLB API."""
    url = f"https://statsapi.mlb.com/api/v1/people/{player_id}"
    try:
        with urllib.request.urlopen(url, timeout=15) as resp:
            data = json.loads(resp.read().decode())
        person = data["people"][0]
        debut = person.get("mlbDebutDate", "")
        last = person.get("lastPlayedDate", "")
        if debut and last:
            start = int(debut[:4])
            end = int(last[:4])
            return list(range(start, end + 1))
    except Exception:
        pass
    return []


# -----------------------------------------------------------------
# Main processing
# -----------------------------------------------------------------

def process_player(name, group, scoring_fn):
    """Process a single player: lookup, fetch all seasons, compute stats."""
    parts = name.rsplit(" ", 1)
    if len(parts) == 2:
        first, last = parts[0], parts[1]
    else:
        first, last = "", parts[0]

    # Handle suffixes
    clean_first = first
    clean_last = last
    for suffix in ["Jr.", "Sr.", "III", "II"]:
        if clean_last == suffix and " " in clean_first:
            fp = clean_first.rsplit(" ", 1)
            clean_first = fp[0]
            clean_last = fp[1]
            break
        if clean_first.endswith(f" {suffix}"):
            clean_first = clean_first.replace(f" {suffix}", "")

    print(f"  Looking up {name} ({clean_last}, {clean_first})...")
    override_key = (clean_last.lower(), clean_first.lower())
    if override_key in PLAYER_ID_OVERRIDES:
        player_id = PLAYER_ID_OVERRIDES[override_key]
        print(f"    Using override key_mlbam: {player_id}")
    else:
        player_id = lookup_mlbam(clean_last, clean_first)
    if player_id is None:
        print(f"    NOT FOUND")
        return None

    print(f"    key_mlbam: {player_id}", flush=True)
    time.sleep(0.5)

    years = get_career_years(player_id)
    if not years:
        print(f"    No career years found")
        return None
    print(f"    Career: {years[0]}–{years[-1]} ({len(years)} seasons)", flush=True)
    time.sleep(0.5)

    all_points = []
    seasons_with_data = 0

    for year in years:
        logs = fetch_game_logs(player_id, year, group)
        if logs:
            seasons_with_data += 1
            for entry in logs:
                pts = scoring_fn(entry["stat"])
                all_points.append(pts)
        time.sleep(0.5)

    if not all_points:
        print(f"    No game log data returned")
        return None

    import statistics
    avg = round(statistics.mean(all_points), 2)
    vol = round(statistics.stdev(all_points), 2) if len(all_points) > 1 else 0.0
    salary = round(avg, 2)

    print(f"    {len(all_points)} games, {seasons_with_data} seasons w/ data, avg={avg}, vol={vol}")

    return {
        "name": name,
        "games": len(all_points),
        "avg_pts": avg,
        "volatility": vol,
        "salary": salary,
        "seasons_with_data": seasons_with_data,
    }


def print_table(title, results):
    results.sort(key=lambda x: x["avg_pts"], reverse=True)
    print(f"\n{'=' * 85}")
    print(f"  {title}")
    print(f"{'=' * 85}")
    print(f"{'Rank':>4}  {'Name':<22} {'Games':>6} {'Avg Pts':>8} {'Volatility':>10} {'Salary':>8} {'Seasons':>8}")
    print(f"{'----':>4}  {'----':<22} {'-----':>6} {'-------':>8} {'----------':>10} {'------':>8} {'-------':>8}")
    for i, r in enumerate(results, 1):
        print(
            f"{i:4d}  {r['name']:<22} {r['games']:6d} {r['avg_pts']:8.2f} "
            f"{r['volatility']:10.2f} {r['salary']:8.2f} {r['seasons_with_data']:8d}"
        )
    print()


# -----------------------------------------------------------------
# Run
# -----------------------------------------------------------------

only_overrides = "--only-overrides" in sys.argv

if not only_overrides:
    # -----------------------------------------------------------------
    # Full run: all 35 players
    # -----------------------------------------------------------------
    print("=" * 85)
    print("  MLB Legends Explorer — ESPN Standard Fantasy Scoring")
    print("=" * 85)

    failed = []

    print(f"\n--- Processing {len(HITTERS)} hitters ---")
    hitter_results = []
    for name in HITTERS:
        result = process_player(name, "hitting", hitter_fantasy_points)
        if result:
            hitter_results.append(result)
        else:
            failed.append((name, "Hitter"))

    print(f"\n--- Processing {len(PITCHERS)} pitchers ---")
    pitcher_results = []
    for name in PITCHERS:
        result = process_player(name, "pitching", pitcher_fantasy_points)
        if result:
            pitcher_results.append(result)
        else:
            failed.append((name, "Pitcher"))

    print_table("TOP HITTERS — ESPN Standard Scoring", hitter_results)
    print_table("TOP PITCHERS — ESPN Standard Scoring", pitcher_results)

    if failed:
        print("--- Players with no data ---")
        for name, ptype in failed:
            print(f"  {name} ({ptype})")
        print()

else:
    # -----------------------------------------------------------------
    # Override verification: test only the 4 hardcoded players
    # -----------------------------------------------------------------
    print("=" * 85)
    print("  OVERRIDE VERIFICATION — Testing 4 hardcoded player IDs")
    print("=" * 85)

    override_players = [
        ("Ken Griffey Jr.", "hitting", hitter_fantasy_points),
        ("Cal Ripken Jr.", "hitting", hitter_fantasy_points),
        ("Randy Johnson", "pitching", pitcher_fantasy_points),
        ("Pedro Martinez", "pitching", pitcher_fantasy_points),
    ]

    override_results = []
    for name, group, scoring_fn in override_players:
        result = process_player(name, group, scoring_fn)
        if result:
            override_results.append(result)
        else:
            print(f"  FAILED: {name}")

    print_table("OVERRIDE VERIFICATION RESULTS", override_results)
