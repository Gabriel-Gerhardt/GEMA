import { renderHook, act } from '@testing-library/react-native';
import type { ReactNode } from 'react';
import { AuthProvider, useAuth } from './AuthContext';

function wrapper({ children }: { children: ReactNode }) {
  return <AuthProvider>{children}</AuthProvider>;
}

describe('AuthContext', () => {
  it('starts signed out', async () => {
    const { result } = await renderHook(() => useAuth(), { wrapper });
    expect(result.current.isSignedIn).toBe(false);
  });

  it('flips to signed in on signIn, and back on signOut', async () => {
    const { result } = await renderHook(() => useAuth(), { wrapper });
    await act(async () => result.current.signIn());
    expect(result.current.isSignedIn).toBe(true);
    await act(async () => result.current.signOut());
    expect(result.current.isSignedIn).toBe(false);
  });
});
