import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';

interface AuthContextValue {
  isSignedIn: boolean;
  signIn: () => void;
  signOut: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

/** Mock, in-memory auth: no backend, no persistence. Submitting Login/Create
 * Account with non-empty fields calls `signIn()` so the app is click-through
 * demoable end to end (see DESIGN.md / plan's "Resolved decisions"). */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [isSignedIn, setIsSignedIn] = useState(false);
  const signIn = useCallback(() => setIsSignedIn(true), []);
  const signOut = useCallback(() => setIsSignedIn(false), []);
  const value = useMemo(() => ({ isSignedIn, signIn, signOut }), [isSignedIn, signIn, signOut]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}
