#!/usr/bin/env python3
"""
Probe the MLB Stats API to explore available game log data
for hitters (Barry Bonds 2001) and pitchers (Roger Clemens 1997).
"""

import subprocess
import sys
import importlib
import json
import time

# Install dependencies if needed
for package, import_name in [("MLB-StatsAPI", "statsapi"), ("pybaseball", "pybaseball")]:
    try:
        importlib.import_module(import_name)
    except ImportError:
        print(f"Installing {package}...")
        subprocess.check_call([sys.executable, "-m", "pip", "install", package, "--quiet"])
        importlib.invalidate_caches()

import statsapi
from pybaseball import playerid_lookup

# ============================================================
# HITTER: Barry Bonds — 2001 (73 HR season)
# ============================================================
print("=" * 70)
print("  HITTER: Barry Bonds — 2001 Season")
print("=" * 70)

print("\nLooking up Barry Bonds via pybaseball...")
bonds_lookup = playerid_lookup("bonds", "barry")
print(bonds_lookup.to_string())
bonds_id = int(bonds_lookup.iloc[0]["key_mlbam"])
print(f"\nkey_mlbam: {bonds_id}")

print(f"\nFetching game logs for personId={bonds_id}, season=2001, group=hitting...")
url = f"https://statsapi.mlb.com/api/v1/people/{bonds_id}/stats?stats=gameLog&group=hitting&season=2001"
print(f"  URL: {url}")
import urllib.request
with urllib.request.urlopen(url) as resp:
    bonds_data = json.loads(resp.read().decode())

bonds_splits = bonds_data["stats"][0]["splits"]
print(f"\nTotal game log entries: {len(bonds_splits)}")

print(f"\n--- Available field names (from first entry) ---")
first = bonds_splits[0]
# Show top-level keys
print(f"  Top-level keys: {list(first.keys())}")
if "stat" in first:
    print(f"  stat fields ({len(first['stat'])}): {list(first['stat'].keys())}")
if "team" in first:
    print(f"  team fields: {list(first['team'].keys())}")
if "opponent" in first:
    print(f"  opponent fields: {list(first['opponent'].keys())}")

print(f"\n--- First 5 game log entries ---")
for entry in bonds_splits[:5]:
    print(json.dumps(entry, indent=2))
    print()

dates = [s["date"] for s in bonds_splits if "date" in s]
if dates:
    print(f"Date range: {min(dates)} to {max(dates)}")

# Rate limit pause
print("\n(waiting 2 seconds before next API call...)")
time.sleep(2)

# ============================================================
# PITCHER: Roger Clemens — 1997 Season
# ============================================================
print("\n" + "=" * 70)
print("  PITCHER: Roger Clemens — 1997 Season")
print("=" * 70)

print("\nLooking up Roger Clemens via pybaseball...")
clemens_lookup = playerid_lookup("clemens", "roger")
print(clemens_lookup.to_string())
clemens_id = int(clemens_lookup.iloc[0]["key_mlbam"])
print(f"\nkey_mlbam: {clemens_id}")

print(f"\nFetching game logs for personId={clemens_id}, season=1997, group=pitching...")
url_p = f"https://statsapi.mlb.com/api/v1/people/{clemens_id}/stats?stats=gameLog&group=pitching&season=1997"
print(f"  URL: {url_p}")
with urllib.request.urlopen(url_p) as resp:
    clemens_data = json.loads(resp.read().decode())

clemens_splits = clemens_data["stats"][0]["splits"]
print(f"\nTotal game log entries: {len(clemens_splits)}")

print(f"\n--- Available field names (from first entry) ---")
first_p = clemens_splits[0]
print(f"  Top-level keys: {list(first_p.keys())}")
if "stat" in first_p:
    print(f"  stat fields ({len(first_p['stat'])}): {list(first_p['stat'].keys())}")
if "team" in first_p:
    print(f"  team fields: {list(first_p['team'].keys())}")
if "opponent" in first_p:
    print(f"  opponent fields: {list(first_p['opponent'].keys())}")

print(f"\n--- First 5 game log entries ---")
for entry in clemens_splits[:5]:
    print(json.dumps(entry, indent=2))
    print()

dates_p = [s["date"] for s in clemens_splits if "date" in s]
if dates_p:
    print(f"Date range: {min(dates_p)} to {max(dates_p)}")

# ============================================================
# HISTORICAL TEST: Willie Mays — 1965 (52 HR season)
# ============================================================
print("\n(waiting 2 seconds before next API call...)")
time.sleep(2)

print("\n" + "=" * 70)
print("  HITTER: Willie Mays — 1965 Season (52 HR)")
print("=" * 70)

print("\nLooking up Willie Mays via pybaseball...")
mays_lookup = playerid_lookup("mays", "willie")
print(mays_lookup.to_string())
mays_id = int(mays_lookup.iloc[0]["key_mlbam"])
print(f"\nkey_mlbam: {mays_id}")

print(f"\nFetching game logs for personId={mays_id}, season=1965, group=hitting...")
url_mays = f"https://statsapi.mlb.com/api/v1/people/{mays_id}/stats?stats=gameLog&group=hitting&season=1965"
print(f"  URL: {url_mays}")
with urllib.request.urlopen(url_mays) as resp:
    mays_data = json.loads(resp.read().decode())

mays_splits = mays_data["stats"][0]["splits"] if mays_data["stats"] and mays_data["stats"][0].get("splits") else []
if mays_splits:
    print(f"\nTotal game log entries: {len(mays_splits)}")
    dates_m = [s["date"] for s in mays_splits if "date" in s]
    if dates_m:
        print(f"Date range: {min(dates_m)} to {max(dates_m)}")
    print(f"\n--- Sample 3 entries ---")
    for entry in mays_splits[:3]:
        print(json.dumps(entry, indent=2))
        print()
else:
    print("\nNO DATA")

# ============================================================
# HISTORICAL TEST: Mickey Mantle — 1956 (Triple Crown year)
# ============================================================
print("(waiting 2 seconds before next API call...)")
time.sleep(2)

print("\n" + "=" * 70)
print("  HITTER: Mickey Mantle — 1956 Season (Triple Crown)")
print("=" * 70)

print("\nLooking up Mickey Mantle via pybaseball...")
mantle_lookup = playerid_lookup("mantle", "mickey")
print(mantle_lookup.to_string())
mantle_id = int(mantle_lookup.iloc[0]["key_mlbam"])
print(f"\nkey_mlbam: {mantle_id}")

print(f"\nFetching game logs for personId={mantle_id}, season=1956, group=hitting...")
url_mantle = f"https://statsapi.mlb.com/api/v1/people/{mantle_id}/stats?stats=gameLog&group=hitting&season=1956"
print(f"  URL: {url_mantle}")
with urllib.request.urlopen(url_mantle) as resp:
    mantle_data = json.loads(resp.read().decode())

mantle_splits = mantle_data["stats"][0]["splits"] if mantle_data["stats"] and mantle_data["stats"][0].get("splits") else []
if mantle_splits:
    print(f"\nTotal game log entries: {len(mantle_splits)}")
    dates_mt = [s["date"] for s in mantle_splits if "date" in s]
    if dates_mt:
        print(f"Date range: {min(dates_mt)} to {max(dates_mt)}")
    print(f"\n--- Sample 3 entries ---")
    for entry in mantle_splits[:3]:
        print(json.dumps(entry, indent=2))
        print()
else:
    print("\nNO DATA")

print()
