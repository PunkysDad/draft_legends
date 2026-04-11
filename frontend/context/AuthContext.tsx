import React, { createContext, useContext, useEffect, useState } from 'react';
import * as authService from '../services/authService';

interface User {
  userId: number;
  coinBalance: number;
}

interface AuthContextValue {
  user: User | null;
  isLoading: boolean;
  signInWithGoogle: () => Promise<void>;
  signInWithApple: () => Promise<void>;
  signOut: () => Promise<void>;
  refreshCoinBalance: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const token = await authService.getToken();
        if (token) {
          setUser({ userId: 0, coinBalance: 0 });
        }
      } finally {
        setIsLoading(false);
      }
    })();
  }, []);

  const signInWithGoogle = async () => {
    const data = await authService.googleSignIn();
    setUser({ userId: data.userId, coinBalance: data.coinBalance });
  };

  const signInWithApple = async () => {
    const data = await authService.appleSignIn();
    setUser({ userId: data.userId, coinBalance: data.coinBalance });
  };

  const signOut = async () => {
    await authService.signOut();
    setUser(null);
  };

  const refreshCoinBalance = async () => {
    const token = await authService.getToken();
    if (!token) return;

    const response = await fetch(`${API_BASE_URL}/api/wallet`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    if (response.ok) {
      const data = await response.json();
      setUser((prev) => (prev ? { ...prev, coinBalance: data.coinBalance } : null));
    }
  };

  return (
    <AuthContext.Provider
      value={{ user, isLoading, signInWithGoogle, signInWithApple, signOut, refreshCoinBalance }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
