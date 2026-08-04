import './global.css';
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
import { AuthProvider } from './src/state/AuthContext';
import { PlansProvider } from './src/state/PlansContext';
import { RootNavigator } from './src/navigation/RootNavigator';
import { colors } from './src/theme/tokens';
import type { PublicStackParamList } from './src/navigation/types';

/** Deep-link config for the unauthenticated scan flow ("someone else's
 * phone opens gema://q/abc123" — the realistic case, per DESIGN.md's flow
 * diagram: "Anyone with the link — no login"). Scoped to PublicStack's
 * shape only: when signed in, RootNavigator mounts the tab shell instead,
 * where a raw external link won't auto-navigate — the signed-in owner's
 * preview of their own plan is covered separately by the in-app tappable
 * links on Plan Detail/Plan Created, not by this config. */
const linking: LinkingOptions<PublicStackParamList> = {
  prefixes: [Linking.createURL('/'), 'gema://'],
  config: { screens: { EmergencyGuide: 'q/:publicId' } },
};

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
          <NavigationContainer linking={linking}>
            <RootNavigator />
            <StatusBar style="dark" />
          </NavigationContainer>
        </PlansProvider>
      </AuthProvider>
    </SafeAreaProvider>
  );
}
