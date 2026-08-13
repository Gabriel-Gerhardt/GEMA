import { Text } from 'react-native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { HomeScreen } from '../screens/HomeScreen';
import { CreatePlanScreen } from '../screens/CreatePlanScreen';
import { ScanScreen } from '../screens/ScanScreen';
import { PlanCreatedScreen } from '../screens/PlanCreatedScreen';
import { GalleryScreen } from '../screens/GalleryScreen';
import { PlanDetailScreen } from '../screens/PlanDetailScreen';
import { EditPlanScreen } from '../screens/EditPlanScreen';
import { ProfileScreen } from '../screens/ProfileScreen';
import { EmergencyGuideScreen } from '../screens/EmergencyGuideScreen';
import { colors } from '../theme/tokens';
import type { AppTabParamList, GalleryStackParamList, HomeStackParamList, ProfileStackParamList } from './types';

const HomeStackNav = createNativeStackNavigator<HomeStackParamList>();
function HomeStack() {
  return (
    <HomeStackNav.Navigator screenOptions={{ headerShown: false }}>
      <HomeStackNav.Screen name="HomeScreen" component={HomeScreen} />
      <HomeStackNav.Screen name="CreatePlan" component={CreatePlanScreen} />
      <HomeStackNav.Screen name="Scan" component={ScanScreen} />
      <HomeStackNav.Screen name="PlanCreated" component={PlanCreatedScreen} />
      <HomeStackNav.Screen name="EmergencyGuide" component={EmergencyGuideScreen} />
    </HomeStackNav.Navigator>
  );
}

const GalleryStackNav = createNativeStackNavigator<GalleryStackParamList>();
function GalleryStack() {
  return (
    <GalleryStackNav.Navigator screenOptions={{ headerShown: false }}>
      <GalleryStackNav.Screen name="GalleryScreen" component={GalleryScreen} />
      <GalleryStackNav.Screen name="PlanDetail" component={PlanDetailScreen} />
      <GalleryStackNav.Screen name="EditPlan" component={EditPlanScreen} />
      <GalleryStackNav.Screen name="CreatePlan" component={CreatePlanScreen} />
      <GalleryStackNav.Screen name="PlanCreated" component={PlanCreatedScreen} />
      <GalleryStackNav.Screen name="EmergencyGuide" component={EmergencyGuideScreen} />
    </GalleryStackNav.Navigator>
  );
}

const ProfileStackNav = createNativeStackNavigator<ProfileStackParamList>();
function ProfileStack() {
  return (
    <ProfileStackNav.Navigator screenOptions={{ headerShown: false }}>
      <ProfileStackNav.Screen name="ProfileScreen" component={ProfileScreen} />
    </ProfileStackNav.Navigator>
  );
}

const Tab = createBottomTabNavigator<AppTabParamList>();

/** The authenticated 3-tab shell (Início/Galeria/Perfil) — each tab hosts
 * its own stack so Gallery can push Plan Detail → Edit, and Home/Gallery
 * can both push Create Plan → Success. */
export function AppTabs() {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarActiveTintColor: colors.text.primary,
        tabBarInactiveTintColor: colors.text.muted,
        tabBarStyle: { backgroundColor: colors.white, borderTopColor: colors.border },
        tabBarLabel: ({ color }) => (
          <Text style={{ color, fontWeight: '600', fontSize: 12 }}>{route.name}</Text>
        ),
        tabBarIcon: () => null,
      })}
    >
      <Tab.Screen name="Início" component={HomeStack} />
      <Tab.Screen name="Galeria" component={GalleryStack} />
      <Tab.Screen name="Perfil" component={ProfileStack} />
    </Tab.Navigator>
  );
}
