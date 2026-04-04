#!/usr/bin/env python3
"""
Probe pybaseball player ID lookups for ambiguous players.
"""

from pybaseball import playerid_lookup

LOOKUPS = [
    ("griffey", "ken"),
    ("ripken", "cal"),
    ("johnson", "randy"),
    ("martinez", "pedro"),
]

for last, first in LOOKUPS:
    print(f"\n{'=' * 70}")
    print(f"  playerid_lookup('{last}', '{first}')")
    print(f"{'=' * 70}")
    df = playerid_lookup(last, first)
    if df.empty:
        print("  NO RESULTS")
    else:
        cols = ["name_last", "name_first", "key_mlbam", "key_bbref", "mlb_played_first", "mlb_played_last"]
        print(df[cols].to_string(index=False))
    print()
