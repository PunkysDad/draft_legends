import { getOpenLeagues, createLeague, joinLeague, autoJoinLeague } from '../services/leagueService';

jest.mock('../services/authService', () => ({
  getToken: jest.fn().mockResolvedValue('test-jwt-token'),
}));

const mockFetch = jest.fn();
global.fetch = mockFetch;

const sampleLeague = {
  leagueId: 10,
  name: 'Test League',
  status: 'OPEN',
  currentTeams: 4,
  maxTeams: 10,
  entryFee: 400,
};

const sampleLeagues = [sampleLeague];

beforeEach(() => {
  jest.clearAllMocks();
});

describe('leagueService', () => {
  test('getOpenLeagues() returns a parsed array on success', async () => {
    mockFetch.mockResolvedValueOnce({ ok: true, json: async () => sampleLeagues });

    const result = await getOpenLeagues();

    expect(result).toEqual(sampleLeagues);
    expect(result).toHaveLength(1);
  });

  test('getOpenLeagues() throws on non-200 response', async () => {
    mockFetch.mockResolvedValueOnce({ ok: false, status: 500 });

    await expect(getOpenLeagues()).rejects.toThrow('Failed to fetch leagues (status 500)');
  });

  test('createLeague() returns the created league on success', async () => {
    mockFetch.mockResolvedValueOnce({ ok: true, json: async () => sampleLeague });

    const result = await createLeague('My League');

    expect(result).toEqual(sampleLeague);
    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/leagues'),
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ name: 'My League' }),
      }),
    );
  });

  test('joinLeague() returns the joined league on success', async () => {
    mockFetch.mockResolvedValueOnce({ ok: true, json: async () => sampleLeague });

    const result = await joinLeague(10);

    expect(result).toEqual(sampleLeague);
    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/leagues/10/join'),
      expect.objectContaining({ method: 'POST' }),
    );
  });

  test('autoJoinLeague() joins the first open league when one exists', async () => {
    // First call: getOpenLeagues
    mockFetch.mockResolvedValueOnce({ ok: true, json: async () => sampleLeagues });
    // Second call: joinLeague
    mockFetch.mockResolvedValueOnce({ ok: true, json: async () => sampleLeague });

    const result = await autoJoinLeague();

    expect(result).toEqual(sampleLeague);
    expect(mockFetch).toHaveBeenCalledTimes(2);
    expect(mockFetch.mock.calls[1][0]).toContain('/api/leagues/10/join');
  });

  test('autoJoinLeague() creates a new league when no open leagues exist', async () => {
    const createdLeague = { ...sampleLeague, leagueId: 99, name: 'League 1234' };
    // First call: getOpenLeagues returns empty
    mockFetch.mockResolvedValueOnce({ ok: true, json: async () => [] });
    // Second call: createLeague
    mockFetch.mockResolvedValueOnce({ ok: true, json: async () => createdLeague });

    const result = await autoJoinLeague();

    expect(result).toEqual(createdLeague);
    expect(mockFetch).toHaveBeenCalledTimes(2);
    expect(mockFetch.mock.calls[1][0]).toContain('/api/leagues');
    expect(mockFetch.mock.calls[1][1].method).toBe('POST');
    expect(JSON.parse(mockFetch.mock.calls[1][1].body).name).toMatch(/^League \d+$/);
  });

  test('all functions attach the Authorization header', async () => {
    mockFetch.mockResolvedValue({ ok: true, json: async () => sampleLeagues });

    await getOpenLeagues();
    expect(mockFetch.mock.calls[0][1].headers).toEqual(
      expect.objectContaining({ Authorization: 'Bearer test-jwt-token' }),
    );

    mockFetch.mockClear();
    mockFetch.mockResolvedValue({ ok: true, json: async () => sampleLeague });

    await createLeague('Test');
    expect(mockFetch.mock.calls[0][1].headers).toEqual(
      expect.objectContaining({ Authorization: 'Bearer test-jwt-token' }),
    );

    mockFetch.mockClear();
    mockFetch.mockResolvedValue({ ok: true, json: async () => sampleLeague });

    await joinLeague(1);
    expect(mockFetch.mock.calls[0][1].headers).toEqual(
      expect.objectContaining({ Authorization: 'Bearer test-jwt-token' }),
    );
  });
});
