import { getToken } from './authService';

const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';

export type Player = {
  playerId: number;
  firstName: string;
  lastName: string;
  position: string;
  photoUrl: string;
  salary: number;
  volatility: number;
  seasonsPlayed: number;
  totalTouchdowns: number;
  totalInterceptions: number | null;
};

async function authHeaders(): Promise<Record<string, string>> {
  const token = await getToken();
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

export async function getPlayers(params?: {
  position?: string;
  sortBy?: string;
  sortDir?: string;
}): Promise<Player[]> {
  const query = new URLSearchParams();
  if (params?.position) query.set('position', params.position);
  if (params?.sortBy) query.set('sortBy', params.sortBy);
  if (params?.sortDir) query.set('sortDir', params.sortDir);
  const qs = query.toString() ? `?${query.toString()}` : '';
  const response = await fetch(`${API_BASE_URL}/api/players${qs}`, {
    headers: await authHeaders(),
  });
  if (!response.ok) {
    throw new Error(`Failed to fetch players (status ${response.status})`);
  }
  return response.json();
}

export async function getPlayer(playerId: number): Promise<Player> {
  const response = await fetch(`${API_BASE_URL}/api/players/${playerId}`, {
    headers: await authHeaders(),
  });
  if (!response.ok) {
    throw new Error(`Failed to fetch player (status ${response.status})`);
  }
  return response.json();
}
