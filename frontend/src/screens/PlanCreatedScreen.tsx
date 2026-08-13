import { ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation, useRoute, type RouteProp } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import * as Clipboard from 'expo-clipboard';
import { Button } from '../components/Button';
import { Card } from '../components/Card';
import { EmptyState } from '../components/EmptyState';
import { QrPlaceholder } from '../components/QrPlaceholder';
import { ErrorState, LoadingState } from '../components/ScreenState';
import { useAsyncResource } from '../hooks/useAsyncResource';
import { usePlans } from '../state/PlansContext';
import { API_BASE_URL } from '../api/config';
import type { HomeStackParamList } from '../navigation/types';

export function PlanCreatedScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<HomeStackParamList>>();
  const route = useRoute<RouteProp<HomeStackParamList, 'PlanCreated'>>();
  const { fetchPlan } = usePlans();
  const planId = route.params.planId;

  const { data: plan, isLoading, error, reload } = useAsyncResource(() => fetchPlan(planId), [planId]);

  if (isLoading && !plan) {
    return (
      <SafeAreaView className="flex-1 bg-cream" edges={['top', 'bottom']}>
        <LoadingState />
      </SafeAreaView>
    );
  }

  if (error && !plan) {
    return (
      <SafeAreaView className="flex-1 bg-cream" edges={['top', 'bottom']}>
        <ErrorState message={error} onRetry={reload} />
      </SafeAreaView>
    );
  }

  if (!plan) {
    return (
      <SafeAreaView className="flex-1 bg-cream" edges={['top', 'bottom']}>
        <EmptyState
          className="flex-1"
          title="Plano não encontrado"
          message="Não conseguimos carregar o plano recém-criado."
          actionLabel="Voltar"
          onAction={() => navigation.goBack()}
        />
      </SafeAreaView>
    );
  }

  // The shareable address is the guide's, which the app itself serves; the API
  // base is only used here as the sensible stand-in until the app has a
  // deployed public origin of its own.
  const link = `${API_BASE_URL}/q/${plan.publicId}`;

  return (
    <SafeAreaView className="flex-1 bg-cream" edges={['top', 'bottom']}>
      <ScrollView contentContainerClassName="flex-grow items-center justify-center px-7 py-6">
        <Card className="w-full items-center p-7">
          <View className="h-[52px] w-[52px] items-center justify-center rounded-full bg-mint-surface">
            <Text className="font-figtreeBold text-[26px] text-success">✓</Text>
          </View>
          <Text className="mt-4 font-figtreeExtrabold text-[22px] text-green-deep">Plano criado</Text>
          <Text className="mt-2 text-center font-figtree text-[14.5px] text-text-muted">
            Compartilhe este link, ou gere um QR a partir dele:
          </Text>
          <View className="mt-4.5">
            <QrPlaceholder size={150} />
          </View>
          <View className="mt-4 w-full rounded-input bg-mint-surface px-3 py-2.5">
            <Text
              role="link"
              aria-label={link}
              onPress={() => navigation.navigate('EmergencyGuide', { publicId: plan.publicId })}
              className="font-figtreeSemibold text-[13px] text-green-deep"
            >
              {link}
            </Text>
          </View>
          <Button onPress={() => Clipboard.setStringAsync(link)} className="mt-4 w-full">
            Copiar link
          </Button>
        </Card>
      </ScrollView>
    </SafeAreaView>
  );
}
