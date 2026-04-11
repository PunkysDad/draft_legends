import { getToken } from './authService';

const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';

export type League = {
  leagueId: number;
  name: string;
  status: string;
  currentTeams: number;
  maxTeams: number;
  entryFee: number;
};

async function authHeaders(): Promise<Record<string, string>> {
  const token = await getToken();
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

export async function getOpenLeagues(): Promise<League[]> {
  const response = await fetch(`${API_BASE_URL}/api/leagues?status=OPEN`, {
    headers: await authHeaders(),
  });
  if (!response.ok) {
    throw new Error(`Failed to fetch leagues (status ${response.status})`);
  }
  return response.json();
}

export async function createLeague(name: string): Promise<League> {
  const response = await fetch(`${API_BASE_URL}/api/leagues`, {
    method: 'POST',
    headers: await authHeaders(),
    body: JSON.stringify({ name }),
  });
  if (!response.ok) {
    throw new Error(`Failed to create league (status ${response.status})`);
  }
  return response.json();
}

export async function joinLeague(leagueId: number): Promise<League> {
  const response = await fetch(`${API_BASE_URL}/api/leagues/${leagueId}/join`, {
    method: 'POST',
    headers: await authHeaders(),
  });
  if (!response.ok) {
    throw new Error(`Failed to join league (status ${response.status})`);
  }
  return response.json();
}

export async function autoJoinLeague(): Promise<League> {
  const openLeagues = await getOpenLeagues();
  if (openLeagues.length > 0) {
    return joinLeague(openLeagues[0].leagueId);
  }
  return createLeague(`League ${Date.now()}`);
}
