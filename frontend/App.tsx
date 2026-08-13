import './global.css';
import { useMemo } from 'react';
import { ActivityIndicator, View } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import { NavigationContainer, type LinkingOptions } from '@react-navigation/native';
import * as Linking from 'expo-linking';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import {
  useFonts,
  Figtree_400Regular,
  Figtree_500Medium,
  Figtree_600SemiBold,
  Figtree_700Bold,
  Figtree_800ExtraBold,
} from '@expo-google-fonts/figtree';
import { AuthProvider, useAuth } from './src/state/AuthContext';
import { PlansProvider } from './src/state/PlansContext';
import { RootNavigator } from './src/navigation/RootNavigator';
import { colors } from './src/theme/tokens';

const PREFIXES = [Linking.createURL('/'), 'gema://'];

/** The two shells are different navigators, so each config is typed against the
 * container's root param list rather than one specific stack. */
type AppLinkingOptions = LinkingOptions<ReactNavigation.RootParamList>;

/** Shape mounted when signed out: PublicStack hosts EmergencyGuide directly. */
const PUBLIC_LINKING: AppLinkingOptions = {
  prefixes: PREFIXES,
  config: {
    screens: {
      Landing: '',
      Login: 'login',
      CreateAccount: 'create-account',
      Onboarding: 'welcome',
      EmergencyGuide: 'q/:publicId',
      // The address the reset email links to.
      ResetPassword: 'redefinir-senha',
      ForgotPassword: 'recuperar-senha',
      NotFound: '*',
    },
  },
};

/**
 * Shape mounted when signed in: the tab shell, where EmergencyGuide lives
 * inside the Galeria tab's stack.
 *
 * Without this the deep link only resolved while signed out — opening
 * `gema://q/abc123` as a logged-in user silently did nothing, because the
 * config described a navigator that wasn't mounted.
 */
const AUTHENTICATED_LINKING: AppLinkingOptions = {
  prefixes: PREFIXES,
  config: {
    screens: {
      Galeria: {
        screens: {
          GalleryScreen: 'planos',
          PlanDetail: 'planos/:planId',
          EditPlan: 'planos/:planId/edit',
          EmergencyGuide: 'q/:publicId',
        },
      },
      Início: { screens: { HomeScreen: 'home' } },
      Perfil: { screens: { ProfileScreen: 'profile' } },
    },
  },
};

/**
 * Sits inside AuthProvider so it can pick the linking config matching the
 * navigator RootNavigator is about to mount. The two shells have different
 * shapes, and a single merged config would declare the same path twice.
 */
function NavigationRoot() {
  const { isSignedIn, isRestoring } = useAuth();
  const linking = useMemo(() => (isSignedIn ? AUTHENTICATED_LINKING : PUBLIC_LINKING), [isSignedIn]);

  // Hold the first render until the stored token has been checked. Mounting the
  // public stack first would flash the landing page at someone who is already
  // signed in, and then yank it away.
  if (isRestoring) {
    return (
      <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.cream }}>
        <ActivityIndicator color={colors.green.primary} />
      </View>
    );
  }

  return (
    <NavigationContainer linking={linking}>
      <RootNavigator />
      <StatusBar style="dark" />
    </NavigationContainer>
  );
}

export default function App() {
  const [fontsLoaded] = useFonts({
    Figtree_400Regular,
    Figtree_500Medium,
    Figtree_600SemiBold,
    Figtree_700Bold,
    Figtree_800ExtraBold,
  });

  if (!fontsLoaded) {
    return (
      <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.cream }}>
        <ActivityIndicator color={colors.green.primary} />
      </View>
    );
  }

  return (
    <SafeAreaProvider>
      <AuthProvider>
        <PlansProvider>
          <NavigationRoot />
        </PlansProvider>
      </AuthProvider>
    </SafeAreaProvider>
  );
}
