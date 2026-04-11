import { getActiveMatchups, createQuickMatch } from '../services/matchupService';

jest.mock('../services/authService', () => ({
  getToken: jest.fn().mockResolvedValue('test-jwt-token'),
}));

const mockFetch = jest.fn();
global.fetch = mockFetch;

const sampleMatchups = [
  {
    matchupId: 1,
    status: 'DRAFTING',
    type: 'QUICK_MATCH',
    opponentName: 'CPU Bot',
    myScore: 0,
    opponentScore: 0,
  },
];

const createdMatchup = {
  matchupId: 99,
  status: 'DRAFTING',
  type: 'QUICK_MATCH',
  opponentName: 'CPU Bot',
  myScore: 0,
  opponentScore: 0,
};

beforeEach(() => {
  jest.clearAllMocks();
});

describe('matchupService', () => {
  test('getActiveMatchups() returns a parsed array on success', async () => {
    mockFetch.mockResolvedValueOnce({ ok: true, json: async () => sampleMatchups });

    const result = await getActiveMatchups();

    expect(result).toEqual(sampleMatchups);
    expect(result).toHaveLength(1);
  });

  test('getActiveMatchups() throws on non-200 response', async () => {
    mockFetch.mockResolvedValueOnce({ ok: false, status: 500 });

    await expect(getActiveMatchups()).rejects.toThrow('Failed to fetch matchups (status 500)');
  });

  test('createQuickMatch() returns the created matchup on success', async () => {
    mockFetch.mockResolvedValueOnce({ ok: true, json: async () => createdMatchup });

    const result = await createQuickMatch();

    expect(result).toEqual(createdMatchup);
    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/matchups'),
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ opponentType: 'CPU' }),
      }),
    );
  });

  test('createQuickMatch() throws on non-200 response', async () => {
    mockFetch.mockResolvedValueOnce({ ok: false, status: 422 });

    await expect(createQuickMatch()).rejects.toThrow('Failed to create matchup (status 422)');
  });

  test('both functions attach the Authorization header', async () => {
    mockFetch.mockResolvedValue({ ok: true, json: async () => sampleMatchups });

    await getActiveMatchups();
    expect(mockFetch.mock.calls[0][1].headers).toEqual(
      expect.objectContaining({ Authorization: 'Bearer test-jwt-token' }),
    );

    mockFetch.mockResolvedValue({ ok: true, json: async () => createdMatchup });

    await createQuickMatch();
    expect(mockFetch.mock.calls[1][1].headers).toEqual(
      expect.objectContaining({ Authorization: 'Bearer test-jwt-token' }),
    );
  });
});
