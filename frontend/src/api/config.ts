/**
 * Base URL of the GEMA API.
 *
 * `EXPO_PUBLIC_`-prefixed variables are inlined at build time, so this is set
 * per environment without a code change. The default is the backend's own
 * local default, which is what `./gradlew bootRun` serves.
 *
 * Note this is the API's address, which is not the same as the app's own —
 * the backend separately needs `APP_PUBLIC_BASE_URL` pointing back here, since
 * that is what a scanned QR code resolves to.
 */
export const API_BASE_URL = (process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080').replace(/\/$/, '');
