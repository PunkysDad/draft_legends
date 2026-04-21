import { getMatchup, submitPick } from '../services/matchupService';

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

const sampleMatchup = {
  matchupId: 10,
  status: 'DRAFTING',
  playerOneSalaryUsed: 0,
  playerTwoSalaryUsed: 0,
  salaryCapPerTeam: 100,
  picks: [],
  gameResults: [],
  playerOneScore: 0,
  playerTwoScore: 0,
  winnerId: null,
};

beforeEach(() => {
  jest.clearAllMocks();
});

describe('matchupService — getMatchup / submitPick', () => {
  test('getMatchup() returns a parsed matchup on success', async () => {
    mockFetch.mockResolvedValueOnce({ ok: true, json: async () => sampleMatchup });

    const result = await getMatchup(10);

    expect(result).toEqual(sampleMatchup);
    expect(mockFetch.mock.calls[0][0]).toContain('/api/matchups/10');
  });

  test('getMatchup() throws on non-200 response', async () => {
    mockFetch.mockResolvedValueOnce({ ok: false, status: 404 });

    await expect(getMatchup(999)).rejects.toThrow('Failed to fetch matchup (status 404)');
  });

  test('submitPick() returns updated matchup on success', async () => {
    const updatedMatchup = {
      ...sampleMatchup,
      picks: [
        { pickNumber: 1, userId: 1, player: samplePlayer, slotType: 'QB' },
        { pickNumber: 2, userId: 2, player: samplePlayer, slotType: 'QB' },
      ],
    };
    mockFetch.mockResolvedValueOnce({ ok: true, json: async () => updatedMatchup });

    const result = await submitPick(10, 1, 'QB');

    expect(result).toEqual(updatedMatchup);
  });

  test('submitPick() sends correct request body', async () => {
    mockFetch.mockResolvedValueOnce({ ok: true, json: async () => sampleMatchup });

    await submitPick(10, 42, 'RB');

    expect(mockFetch.mock.calls[0][1].method).toBe('POST');
    expect(JSON.parse(mockFetch.mock.calls[0][1].body)).toEqual({ playerId: 42, slotType: 'RB' });
    expect(mockFetch.mock.calls[0][0]).toContain('/api/matchups/10/pick');
  });

  test('submitPick() throws on non-200 response', async () => {
    mockFetch.mockResolvedValueOnce({ ok: false, status: 422 });

    await expect(submitPick(10, 1, 'QB')).rejects.toThrow('Failed to submit pick (status 422)');
  });

  test('both functions attach the Authorization header', async () => {
    mockFetch.mockResolvedValue({ ok: true, json: async () => sampleMatchup });

    await getMatchup(10);
    expect(mockFetch.mock.calls[0][1].headers).toEqual(
      expect.objectContaining({ Authorization: 'Bearer test-jwt-token' }),
    );

    mockFetch.mockClear();
    mockFetch.mockResolvedValue({ ok: true, json: async () => sampleMatchup });

    await submitPick(10, 1, 'QB');
    expect(mockFetch.mock.calls[0][1].headers).toEqual(
      expect.objectContaining({ Authorization: 'Bearer test-jwt-token' }),
    );
  });
});
