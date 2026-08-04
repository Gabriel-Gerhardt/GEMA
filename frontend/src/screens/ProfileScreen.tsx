import { Alert, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Button } from '../components/Button';
import { usePlans } from '../state/PlansContext';
import { useAuth } from '../state/AuthContext';
import { MOCK_PROFILE } from '../mocks/profile';

export function ProfileScreen() {
  const { plans } = usePlans();
  const { signOut } = useAuth();
  const initial = MOCK_PROFILE.name.charAt(0);

  // No backend/account record exists yet to actually delete — signing out
  // is the deliberate mock-time stand-in. Plans aren't touched; a real
  // implementation would need this to actually erase account + plan data.
  function handleDeleteAccount() {
    Alert.alert('Excluir conta?', 'Essa ação não pode ser desfeita.', [
      { text: 'Cancelar', style: 'cancel' },
      { text: 'Excluir', style: 'destructive', onPress: signOut },
    ]);
  }

  return (
    <SafeAreaView className="flex-1 bg-cream" edges={['bottom']}>
      <ScrollView contentContainerClassName="px-6 py-7">
        <View className="flex-row items-center gap-4">
          <View className="h-16 w-16 items-center justify-center rounded-tl-[22px] rounded-tr-[8px] rounded-br-[22px] rounded-bl-[8px] bg-mint-surface">
            <Text className="font-figtreeExtrabold text-[24px] text-green-deep">{initial}</Text>
          </View>
          <View>
            <Text className="font-figtreeExtrabold text-[22px] text-green-deep">{MOCK_PROFILE.name}</Text>
            <Text className="mt-0.5 font-figtree text-[14px] text-text-muted">{MOCK_PROFILE.email}</Text>
          </View>
        </View>

        <View className="mt-6 border-t border-border pt-5.5">
          <Text className="font-figtreeSemibold text-[13.5px] text-text-muted">Planos criados</Text>
          <Text className="mt-1.5 font-figtreeExtrabold text-[34px] text-text-primary">{plans.length}</Text>
        </View>

        <View className="mt-6 gap-3.5 border-t border-border pt-5.5">
          {/* No edit-profile screen is in the design handoff's 13-screen scope — rendered for fidelity, no destination yet. */}
          <Button variant="secondary" onPress={() => {}} className="w-full">
            Editar perfil
          </Button>
          <Text
            role="link"
            aria-label="Excluir conta"
            onPress={handleDeleteAccount}
            className="self-center font-figtreeSemibold text-[13px] text-danger"
          >
            Excluir conta
          </Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
