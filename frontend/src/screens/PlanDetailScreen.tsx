import { ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation, useRoute, type RouteProp } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Button } from '../components/Button';
import { EmptyState } from '../components/EmptyState';
import { QrPlaceholder } from '../components/QrPlaceholder';
import { SectionReadItem } from '../components/SectionReadItem';
import { StatusDot } from '../components/StatusDot';
import { usePlans } from '../state/PlansContext';
import type { GalleryStackParamList } from '../navigation/types';

export function PlanDetailScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<GalleryStackParamList>>();
  const route = useRoute<RouteProp<GalleryStackParamList, 'PlanDetail'>>();
  const { getPlan } = usePlans();
  const plan = getPlan(route.params.planId);

  // Rendering `null` here left a blank screen whenever the plan had just been
  // deleted (or the id was stale) rather than telling the user anything.
  if (!plan) {
    return (
      <SafeAreaView className="flex-1 bg-cream" edges={['bottom']}>
        <EmptyState
          className="flex-1"
          title="Plano não encontrado"
          message="Este plano foi excluído ou não existe mais."
          actionLabel="Voltar à galeria"
          onAction={() => navigation.navigate('GalleryScreen')}
        />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView className="flex-1 bg-cream" edges={['bottom']}>
      <ScrollView contentContainerClassName="px-6 py-6">
        <View className="flex-row items-center justify-between">
          <Text role="link" aria-label="← Galeria" onPress={() => navigation.goBack()} className="font-figtreeSemibold text-[13px] text-green-deep underline">
            ← Galeria
          </Text>
          <View className="flex-row items-center gap-1.5">
            <StatusDot active={plan.active} />
            <Text className="font-figtreeSemibold text-[12px] text-text-muted">{plan.active ? 'Ativo' : 'Inativo'}</Text>
          </View>
        </View>

        <Text className="mt-3.5 font-figtreeExtrabold text-[28px] tracking-[-0.02em] text-green-deep">{plan.title}</Text>
        <Text className="mt-1.5 font-figtree text-[12.5px] text-text-muted">
          Criado {plan.createdAt} ·{' '}
          <Text
            role="link"
            aria-label={`gema.app/q/${plan.publicId}`}
            onPress={() => navigation.navigate('EmergencyGuide', { publicId: plan.publicId })}
            className="underline"
          >
            gema.app/q/{plan.publicId}
          </Text>
        </Text>

        <View className="mt-4.5 items-center">
          <QrPlaceholder size={128} />
        </View>

        <View className="mt-5 gap-2.5">
          {plan.sections.map((section) => (
            <SectionReadItem key={section.id} title={section.title} content={section.content} />
          ))}
        </View>

        <Button onPress={() => navigation.navigate('EditPlan', { planId: plan.id })} className="mt-4.5 w-full">
          Editar plano
        </Button>
      </ScrollView>
    </SafeAreaView>
  );
}
