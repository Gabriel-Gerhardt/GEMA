import { Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Button } from '../components/Button';
import { EmptyState } from '../components/EmptyState';
import { ErrorState, LoadingState } from '../components/ScreenState';
import { QrPlaceholder } from '../components/QrPlaceholder';
import { StatusDot } from '../components/StatusDot';
import { usePlans } from '../state/PlansContext';
import type { GalleryStackParamList } from '../navigation/types';

export function GalleryScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<GalleryStackParamList>>();
  const { plans, isLoading, error, reload } = usePlans();

  return (
    <SafeAreaView className="flex-1 bg-cream" edges={['bottom']}>
      <ScrollView contentContainerClassName="flex-grow px-6 py-7">
        <Text className="font-figtreeBold text-[12px] uppercase tracking-[0.14em] text-green-primary">
          Seus planos
        </Text>
        <Text className="mt-3 font-figtreeExtrabold text-[28px] tracking-[-0.02em] text-green-deep">Galeria</Text>

        {isLoading && plans.length === 0 ? (
          <View className="py-12">
            <LoadingState label="Carregando seus planos…" />
          </View>
        ) : error && plans.length === 0 ? (
          <View className="py-8">
            <ErrorState message={error} onRetry={() => void reload()} />
          </View>
        ) : plans.length === 0 ? (
          // Deleting every plan used to leave the screen blank below the title.
          <EmptyState
            className="my-8"
            markSize={44}
            title="Nenhum plano ainda"
            message="Crie seu primeiro plano para gerar um guia que pode ser compartilhado por QR."
          />
        ) : (
          <View className="mt-5 gap-3">
            {plans.map((plan) => (
              <Pressable
                key={plan.id}
                role="button"
                aria-label={plan.title}
                onPress={() => navigation.navigate('PlanDetail', { planId: plan.id })}
                className={`flex-row items-center gap-3.5 rounded-tl-card-md rounded-tr-card-md-alt rounded-br-card-md rounded-bl-card-md-alt border border-border bg-white p-3.5 ${plan.active ? '' : 'opacity-[.72]'}`}
              >
                {/* The design calls for a QR swatch thumbnail here, not a blank tile. */}
                <QrPlaceholder size={52} />
                <View className="min-w-0 flex-1">
                  <Text className="font-figtreeBold text-[16px] text-text-primary">{plan.title}</Text>
                  <Text className="mt-0.5 font-figtree text-[13px] text-text-muted">Criado {plan.createdAt}</Text>
                </View>
                <StatusDot active={plan.active} />
              </Pressable>
            ))}
          </View>
        )}

        <Button onPress={() => navigation.navigate('CreatePlan')} className="mt-5 w-full">
          + Criar plano
        </Button>
      </ScrollView>
    </SafeAreaView>
  );
}
