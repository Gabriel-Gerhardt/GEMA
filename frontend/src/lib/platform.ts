import { Platform } from 'react-native';

/**
 * Whether this is the web build.
 *
 * Wrapped in a function rather than read inline so a test can state which
 * platform it is exercising: the Jest preset runs every suite once per
 * platform, and assigning to `Platform.OS` does not survive that.
 */
export function isWebPlatform(): boolean {
  return Platform.OS === 'web';
}
