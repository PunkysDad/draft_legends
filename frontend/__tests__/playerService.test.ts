import { getPlayers, getPlayer } from '../services/playerService';

jest.mock('../services/authService', () => ({
  getToken: jest.fn().mockResolvedValue('test-jwt-token'),
}));

const mockFetch = jest.fn();
global.fetch = mockFetch;

const samplePlayer = {
  playerId: 1,
  firstName: 'Patrick',
  lastName: 'Mahomes',
  position: 'QB',
  photoUrl: '',
  salary: 42.5,
  volatility: 18.3,
  seasonsPlayed: 7,
  totalTouchdowns: 282,
  totalInterceptions: 89,
};

const samplePlayers = [samplePlayer];

beforeEach(() => {
  jest.clearAllMocks();
});

describe('playerService', () => {
  test('getPlayers() returns a parsed array on success', async () => {
    mockFetch.mockResolvedValueOnce({ ok: true, json: async () => samplePlayers });

    const result = await getPlayers();

    expect(result).toEqual(samplePlayers);
    expect(result).toHaveLength(1);
  });

  test('getPlayers() with position filter sends correct query param', async () => {
    mockFetch.mockResolvedValueOnce({ ok: true, json: async () => samplePlayers });

    await getPlayers({ position: 'QB' });

    expect(mockFetch.mock.calls[0][0]).toContain('position=QB');
  });

  test('getPlayers() with sortBy and sortDir sends correct query params', async () => {
    mockFetch.mockResolvedValueOnce({ ok: true, json: async () => samplePlayers });

    await getPlayers({ sortBy: 'salary', sortDir: 'desc' });

    expect(mockFetch.mock.calls[0][0]).toContain('sortBy=salary');
    expect(mockFetch.mock.calls[0][0]).toContain('sortDir=desc');
  });

  test('getPlayers() throws on non-200 response', async () => {
    mockFetch.mockResolvedValueOnce({ ok: false, status: 500 });

    await expect(getPlayers()).rejects.toThrow('Failed to fetch players (status 500)');
  });

  test('getPlayer() returns a single player on success', async () => {
    mockFetch.mockResolvedValueOnce({ ok: true, json: async () => samplePlayer });

    const result = await getPlayer(1);

    expect(result).toEqual(samplePlayer);
    expect(mockFetch.mock.calls[0][0]).toContain('/api/players/1');
  });

  test('getPlayer() throws on non-200 response', async () => {
    mockFetch.mockResolvedValueOnce({ ok: false, status: 404 });

    await expect(getPlayer(999)).rejects.toThrow('Failed to fetch player (status 404)');
  });

  test('both functions attach the Authorization header', async () => {
    mockFetch.mockResolvedValue({ ok: true, json: async () => samplePlayers });

    await getPlayers();
    expect(mockFetch.mock.calls[0][1].headers).toEqual(
      expect.objectContaining({ Authorization: 'Bearer test-jwt-token' }),
    );

    mockFetch.mockClear();
    mockFetch.mockResolvedValue({ ok: true, json: async () => samplePlayer });

    await getPlayer(1);
    expect(mockFetch.mock.calls[0][1].headers).toEqual(
      expect.objectContaining({ Authorization: 'Bearer test-jwt-token' }),
    );
  });
});
