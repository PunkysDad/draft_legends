import { googleSignIn, appleSignIn, getToken, signOut } from '../services/authService';

// Mock expo-secure-store
const mockStore: Record<string, string> = {};
jest.mock('expo-secure-store', () => ({
  setItemAsync: jest.fn(async (key: string, value: string) => {
    mockStore[key] = value;
  }),
  getItemAsync: jest.fn(async (key: string) => mockStore[key] ?? null),
  deleteItemAsync: jest.fn(async (key: string) => {
    delete mockStore[key];
  }),
}));

// Mock Google Sign-In
jest.mock('@react-native-google-signin/google-signin', () => ({
  GoogleSignin: {
    configure: jest.fn(),
    hasPlayServices: jest.fn().mockResolvedValue(true),
    signIn: jest.fn().mockResolvedValue({ data: { idToken: 'google-id-token' } }),
  },
}));

// Mock Apple Authentication
jest.mock('expo-apple-authentication', () => ({
  signInAsync: jest.fn().mockResolvedValue({ identityToken: 'apple-identity-token' }),
  AppleAuthenticationScope: { FULL_NAME: 0, EMAIL: 1 },
}));

// Mock fetch
const mockFetch = jest.fn();
global.fetch = mockFetch;

const authResponse = { token: 'jwt-token-123', userId: 42, coinBalance: 500 };

beforeEach(() => {
  jest.clearAllMocks();
  for (const key of Object.keys(mockStore)) delete mockStore[key];
  mockFetch.mockResolvedValue({
    ok: true,
    json: async () => authResponse,
  });
});

describe('authService', () => {
  test('googleSignIn() stores the token on success', async () => {
    const result = await googleSignIn();

    expect(result).toEqual(authResponse);
    expect(mockStore['auth_token']).toBe('jwt-token-123');
    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/auth/google'),
      expect.objectContaining({ method: 'POST' }),
    );
  });

  test('appleSignIn() stores the token on success', async () => {
    const result = await appleSignIn();

    expect(result).toEqual(authResponse);
    expect(mockStore['auth_token']).toBe('jwt-token-123');
    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/auth/apple'),
      expect.objectContaining({ method: 'POST' }),
    );
  });

  test('getToken() returns null when no token stored', async () => {
    const token = await getToken();
    expect(token).toBeNull();
  });

  test('getToken() returns the token when one is stored', async () => {
    mockStore['auth_token'] = 'stored-jwt';
    const token = await getToken();
    expect(token).toBe('stored-jwt');
  });

  test('signOut() deletes the stored token', async () => {
    mockStore['auth_token'] = 'jwt-to-delete';
    await signOut();
    expect(mockStore['auth_token']).toBeUndefined();
  });
});
