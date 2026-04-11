import { useState } from 'react';
import { View, Text, StyleSheet, ActivityIndicator, Platform } from 'react-native';
import { GoogleSigninButton } from '@react-native-google-signin/google-signin';
import * as AppleAuthentication from 'expo-apple-authentication';
import { useAuth } from '../context/AuthContext';

export default function LoginScreen() {
  const { signInWithGoogle, signInWithApple } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleGoogleSignIn = async () => {
    setLoading(true);
    setError(null);
    try {
      await signInWithGoogle();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Google sign-in failed');
    } finally {
      setLoading(false);
    }
  };

  const handleAppleSignIn = async () => {
    setLoading(true);
    setError(null);
    try {
      await signInWithApple();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Apple sign-in failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Legends Clash</Text>
      <Text style={styles.tagline}>Draft Legends. Claim Glory.</Text>

      <View style={styles.buttonContainer}>
        <GoogleSigninButton
          size={GoogleSigninButton.Size.Wide}
          color={GoogleSigninButton.Color.Dark}
          onPress={handleGoogleSignIn}
          disabled={loading}
        />

        {Platform.OS === 'ios' && (
          <AppleAuthentication.AppleAuthenticationButton
            buttonType={AppleAuthentication.AppleAuthenticationButtonType.SIGN_IN}
            buttonStyle={AppleAuthentication.AppleAuthenticationButtonStyle.WHITE}
            cornerRadius={5}
            style={styles.appleButton}
            onPress={handleAppleSignIn}
          />
        )}
      </View>

      {loading && <ActivityIndicator size="large" color="#FFD700" style={styles.loader} />}
      {error && <Text style={styles.error}>{error}</Text>}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#000',
    padding: 24,
  },
  title: {
    color: '#FFD700',
    fontSize: 36,
    fontWeight: 'bold',
    marginBottom: 8,
  },
  tagline: {
    color: '#999',
    fontSize: 16,
    marginBottom: 48,
  },
  buttonContainer: {
    width: '100%',
    alignItems: 'center',
    gap: 16,
  },
  appleButton: {
    width: 312,
    height: 44,
  },
  loader: {
    marginTop: 24,
  },
  error: {
    color: '#ff4444',
    fontSize: 14,
    marginTop: 16,
    textAlign: 'center',
  },
});
