import { GoogleSignin } from '@react-native-google-signin/google-signin';
import * as AppleAuthentication from 'expo-apple-authentication';
import * as SecureStore from 'expo-secure-store';

const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';
const TOKEN_KEY = 'auth_token';

GoogleSignin.configure({
  webClientId: process.env.EXPO_PUBLIC_GOOGLE_CLIENT_ID,
});

export interface AuthResponse {
  token: string;
  userId: number;
  coinBalance: number;
}

export async function googleSignIn(): Promise<AuthResponse> {
  try {
    await GoogleSignin.hasPlayServices();
    const signInResult = await GoogleSignin.signIn();
    const idToken = signInResult.data?.idToken;
    if (!idToken) {
      throw new Error('No ID token returned from Google Sign-In');
    }

    const response = await fetch(`${API_BASE_URL}/api/auth/google`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ idToken }),
    });

    if (!response.ok) {
      throw new Error(`Google auth failed with status ${response.status}`);
    }

    const data: AuthResponse = await response.json();
    await SecureStore.setItemAsync(TOKEN_KEY, data.token);
    return data;
  } catch (error) {
    throw new Error(`Google Sign-In failed: ${error instanceof Error ? error.message : String(error)}`);
  }
}

export async function appleSignIn(): Promise<AuthResponse> {
  try {
    const credential = await AppleAuthentication.signInAsync({
      requestedScopes: [
        AppleAuthentication.AppleAuthenticationScope.FULL_NAME,
        AppleAuthentication.AppleAuthenticationScope.EMAIL,
      ],
    });

    const identityToken = credential.identityToken;
    if (!identityToken) {
      throw new Error('No identity token returned from Apple Sign-In');
    }

    const response = await fetch(`${API_BASE_URL}/api/auth/apple`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ identityToken }),
    });

    if (!response.ok) {
      throw new Error(`Apple auth failed with status ${response.status}`);
    }

    const data: AuthResponse = await response.json();
    await SecureStore.setItemAsync(TOKEN_KEY, data.token);
    return data;
  } catch (error) {
    throw new Error(`Apple Sign-In failed: ${error instanceof Error ? error.message : String(error)}`);
  }
}

export async function getToken(): Promise<string | null> {
  return SecureStore.getItemAsync(TOKEN_KEY);
}

export async function signOut(): Promise<void> {
  await SecureStore.deleteItemAsync(TOKEN_KEY);
}
