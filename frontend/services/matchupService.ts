import { getToken } from './authService';
import type { Player } from './playerService';

const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';

export type MatchupSummary = {
  matchupId: number;
  status: string;
  type: 'QUICK_MATCH' | 'CLASSIC_LEAGUE';
  opponentName: string;
  myScore: number;
  opponentScore: number;
};

async function authHeaders(): Promise<Record<string, string>> {
  const token = await getToken();
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

export async function getActiveMatchups(): Promise<MatchupSummary[]> {
  const response = await fetch(`${API_BASE_URL}/api/matchups?status=ACTIVE`, {
    headers: await authHeaders(),
  });
  if (!response.ok) {
    throw new Error(`Failed to fetch matchups (status ${response.status})`);
  }
  return response.json();
}

export type DraftPick = {
  pickNumber: number;
  userId: number;
  player: Player;
  slotType: 'QB' | 'RB' | 'WR';
};

export type GameResult = {
  userId: number;
  player: Player;
  gameDate: string;
  position: string;
  passAttempts: number | null;
  passCompletions: number | null;
  passYards: number | null;
  passTds: number | null;
  interceptions: number | null;
  passerRating: number | null;
  sacks: number | null;
  rushAttempts: number | null;
  rushYards: number | null;
  rushTds: number | null;
  receptions: number | null;
  recYards: number | null;
  recTds: number | null;
  wrReceptions: number | null;
  wrYards: number | null;
  wrTds: number | null;
  fantasyPoints: number;
};

export type Matchup = {
  matchupId: number;
  status: 'DRAFTING' | 'COMPLETE';
  playerOneSalaryUsed: number;
  playerTwoSalaryUsed: number;
  salaryCapPerTeam: number;
  picks: DraftPick[];
  gameResults: GameResult[];
  playerOneScore: number;
  playerTwoScore: number;
  winnerId: number | null;
};

export async function getMatchup(matchupId: number): Promise<Matchup> {
  const response = await fetch(`${API_BASE_URL}/api/matchups/${matchupId}`, {
    headers: await authHeaders(),
  });
  if (!response.ok) {
    throw new Error(`Failed to fetch matchup (status ${response.status})`);
  }
  return response.json();
}

export async function submitPick(
  matchupId: number,
  playerId: number,
  slotType: string,
): Promise<Matchup> {
  const response = await fetch(`${API_BASE_URL}/api/matchups/${matchupId}/pick`, {
    method: 'POST',
    headers: await authHeaders(),
    body: JSON.stringify({ playerId, slotType }),
  });
  if (!response.ok) {
    throw new Error(`Failed to submit pick (status ${response.status})`);
  }
  return response.json();
}

export async function createQuickMatch(): Promise<MatchupSummary> {
  const response = await fetch(`${API_BASE_URL}/api/matchups`, {
    method: 'POST',
    headers: await authHeaders(),
    body: JSON.stringify({ opponentType: 'CPU' }),
  });
  if (!response.ok) {
    throw new Error(`Failed to create matchup (status ${response.status})`);
  }
  return response.json();
}
