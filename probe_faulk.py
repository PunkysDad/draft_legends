#!/usr/bin/env python3
"""
Probe raw nfl-data-py data for Marshall Faulk to inspect available fields.
"""

import nfl_data_py as nfl
import pandas as pd

pd.set_option("display.max_columns", None)
pd.set_option("display.width", None)
pd.set_option("display.max_colwidth", 30)

YEARS = list(range(1999, 2007))

print(f"Fetching weekly data for {YEARS[0]}–{YEARS[-1]}...")
df = nfl.import_weekly_data(YEARS)

faulk = df[df["player_display_name"] == "Marshall Faulk"]

print(f"\n{'=' * 60}")
print(f"  Total rows found: {len(faulk)}")
print(f"{'=' * 60}")

print(f"\n--- All column names ({len(df.columns)}) ---")
for i, col in enumerate(df.columns):
    print(f"  {i+1:3d}. {col}")

print(f"\n--- First 10 rows ---")
print(faulk.head(10).to_string())

print(f"\n--- Seasons present ---")
seasons = sorted(faulk["season"].unique())
print(f"  {seasons}")

print(f"\n--- Games per season ---")
games_per_season = faulk.groupby("season").size().reset_index(name="games")
print(games_per_season.to_string(index=False))
