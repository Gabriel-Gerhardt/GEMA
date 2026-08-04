import { useAuth } from '../state/AuthContext';
import { PublicStack } from './PublicStack';
import { AppTabs } from './AppTabs';

/** Top-level switch on the mock auth state — signed out sees the public
 * stack (landing, onboarding, login, the public Emergency Guide View),
 * signed in sees the 3-tab app shell. Matches DESIGN.md's auth-gate list. */
export function RootNavigator() {
  const { isSignedIn } = useAuth();
  return isSignedIn ? <AppTabs /> : <PublicStack />;
}
