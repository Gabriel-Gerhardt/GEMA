import { Platform } from 'react-native';
import * as SecureStore from 'expo-secure-store';

const TOKEN_KEY = 'gema.auth.token';

/**
 * Where the session token lives.
 *
 * On native this is the OS keystore/keychain via `expo-secure-store`. That
 * module has no web implementation in SDK 57 — its web entry point exports an
 * empty object — so the web build falls back to `localStorage`, which is the
 * best a browser offers without a backend session cookie. The token is a bearer
 * credential either way, so it is worth knowing that the web path is the weaker
 * one: anything that can run script on the page can read it.
 */
export const tokenStorage = {
  async get(): Promise<string | null> {
    if (Platform.OS === 'web') {
      try {
        return globalThis.localStorage?.getItem(TOKEN_KEY) ?? null;
      } catch {
        // Private browsing modes can throw on access rather than return null.
        return null;
      }
    }
    return SecureStore.getItemAsync(TOKEN_KEY);
  },

  async set(token: string): Promise<void> {
    if (Platform.OS === 'web') {
      try {
        globalThis.localStorage?.setItem(TOKEN_KEY, token);
      } catch {
        // Storage unavailable: the session simply won't survive a reload.
      }
      return;
    }
    await SecureStore.setItemAsync(TOKEN_KEY, token);
  },

  async clear(): Promise<void> {
    if (Platform.OS === 'web') {
      try {
        globalThis.localStorage?.removeItem(TOKEN_KEY);
      } catch {
        // Nothing to do — treat as already gone.
      }
      return;
    }
    await SecureStore.deleteItemAsync(TOKEN_KEY);
  },
};
