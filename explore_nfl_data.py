#!/usr/bin/env python3
"""
Explore legendary RB and WR candidates from nfl-data-py (1999–2010).
Computes fantasy points using Legends Clash scoring rules and prints
top 30 at each position for browsing.
"""

import subprocess
import sys

import importlib

try:
    import nfl_data_py as nfl
except ImportError:
    print("Installing nfl-data-py...")
    subprocess.check_call([sys.executable, "-m", "pip", "install", "nfl-data-py", "--quiet"])
    importlib.invalidate_caches()
    import nfl_data_py as nfl

import pandas as pd

pd.set_option("display.max_rows", None)
pd.set_option("display.width", 120)

YEARS = list(range(1999, 2011))
MIN_GAMES = 50

print(f"Fetching weekly data for {YEARS[0]}–{YEARS[-1]}...")
df = nfl.import_weekly_data(YEARS)

# Fill NaN stat columns with 0
stat_cols = [
    "carries", "rushing_yards", "rushing_tds",
    "receptions", "receiving_yards", "receiving_tds",
]
for col in stat_cols:
    if col in df.columns:
        df[col] = df[col].fillna(0)


def compute_rb(group: pd.DataFrame) -> pd.Series:
    """RB scoring: rush_yards/10 + rush_tds*6 + receptions*0.5 + rec_yards/10 + rec_tds*6"""
    pts = (
        group["rushing_yards"] / 10
        + group["rushing_tds"] * 6
        + group["receptions"] * 0.5
        + group["receiving_yards"] / 10
        + group["receiving_tds"] * 6
    )
    return pd.Series({
        "name": group["player_display_name"].iloc[0],
        "years_active": f"{int(group['season'].min())}–{int(group['season'].max())}",
        "games": len(group),
        "avg_pts": round(pts.mean(), 2),
        "volatility": round(pts.std(), 2),
    })


def compute_wr(group: pd.DataFrame) -> pd.Series:
    """WR scoring: receiving_yards/10 + receiving_tds*6 + receptions*0.5"""
    pts = (
        group["receiving_yards"] / 10
        + group["receiving_tds"] * 6
        + group["receptions"] * 0.5
    )
    return pd.Series({
        "name": group["player_display_name"].iloc[0],
        "years_active": f"{int(group['season'].min())}–{int(group['season'].max())}",
        "games": len(group),
        "avg_pts": round(pts.mean(), 2),
        "volatility": round(pts.std(), 2),
    })


def print_table(title: str, data: pd.DataFrame, top_n: int = 30):
    filtered = data[data["games"] >= MIN_GAMES].sort_values("avg_pts", ascending=False).head(top_n)
    filtered = filtered.reset_index(drop=True)
    filtered.index += 1
    filtered.index.name = "Rank"
    filtered.columns = ["Name", "Years Active", "Games", "Avg Pts", "Volatility"]

    print(f"\n{'=' * 75}")
    print(f"  {title} (min {MIN_GAMES} games, {YEARS[0]}–{YEARS[-1]})")
    print(f"{'=' * 75}")
    print(filtered.to_string())
    print()


# --- RBs ---
rbs = df[df["position"] == "RB"]
# Filter to games where the player actually had stats (carried or caught)
rbs = rbs[(rbs["carries"] > 0) | (rbs["receptions"] > 0)]
rb_stats = rbs.groupby("player_id").apply(compute_rb)

# --- WRs ---
wrs = df[df["position"] == "WR"]
wrs = wrs[(wrs["receptions"] > 0) | (wrs["receiving_yards"] > 0)]
wr_stats = wrs.groupby("player_id").apply(compute_wr)

print_table("TOP 30 RUNNING BACKS", rb_stats)
print_table("TOP 30 WIDE RECEIVERS", wr_stats)
