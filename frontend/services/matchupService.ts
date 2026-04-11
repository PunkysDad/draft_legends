import { getToken } from './authService';

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
