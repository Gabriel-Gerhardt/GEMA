import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import * as api from '../api/endpoints';
import { tokenStorage } from '../api/tokenStorage';
import type { UserDetailsResponse } from '../api/types';

interface AuthContextValue {
  isSignedIn: boolean;
  /** True while the stored token is being checked on boot, before the first render decision. */
  isRestoring: boolean;
  user: UserDetailsResponse | null;
  signIn: (username: string, password: string) => Promise<void>;
  register: (username: string, password: string, name?: string) => Promise<void>;
  signOut: () => Promise<void>;
  refreshUser: () => Promise<void>;
  /** Passed to API calls so a 401 anywhere tears the session down in one place. */
  onUnauthorized: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserDetailsResponse | null>(null);
  const [isRestoring, setIsRestoring] = useState(true);
  // Guards against a late response from a signed-out session writing state back.
  const mounted = useRef(true);

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  const clearSession = useCallback(async () => {
    await tokenStorage.clear();
    if (mounted.current) setUser(null);
  }, []);

  /** A 401 on any call means the token is gone or expired; drop it rather than
   * leaving the app in a half-signed-in state making calls that keep failing. */
  const onUnauthorized = useCallback(() => {
    void clearSession();
  }, [clearSession]);

  // Restore the session on boot. A stored token may have expired while the app
  // was closed, so it is only trusted once /me accepts it.
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const token = await tokenStorage.get();
        if (!token) return;
        const me = await api.getCurrentUser();
        if (!cancelled && mounted.current) setUser(me);
      } catch {
        // Expired, revoked, or the API is unreachable. Either way this is not a
        // usable session; start signed out rather than blocking the app.
        await tokenStorage.clear();
      } finally {
        if (!cancelled && mounted.current) setIsRestoring(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const adopt = useCallback(async (token: string) => {
    await tokenStorage.set(token);
    // Read the account back rather than trusting the auth response's shape:
    // /me is the one place that knows the plan count Profile renders.
    const me = await api.getCurrentUser();
    if (mounted.current) setUser(me);
  }, []);

  const signIn = useCallback(
    async (username: string, password: string) => {
      const auth = await api.login(username, password);
      await adopt(auth.token);
    },
    [adopt],
  );

  const register = useCallback(
    async (username: string, password: string, name?: string) => {
      const auth = await api.register(username, password, name);
      await adopt(auth.token);
    },
    [adopt],
  );

  const signOut = useCallback(async () => {
    await clearSession();
  }, [clearSession]);

  const refreshUser = useCallback(async () => {
    try {
      const me = await api.getCurrentUser({ onUnauthorized });
      if (mounted.current) setUser(me);
    } catch {
      // Leave the last known account on screen rather than blanking Profile
      // because one refresh failed.
    }
  }, [onUnauthorized]);

  const value = useMemo(
    () => ({
      isSignedIn: user !== null,
      isRestoring,
      user,
      signIn,
      register,
      signOut,
      refreshUser,
      onUnauthorized,
    }),
    [user, isRestoring, signIn, register, signOut, refreshUser, onUnauthorized],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}
