import { Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { SunflowerMark } from '../components/SunflowerMark';
import { Card } from '../components/Card';
import { usePlans } from '../state/PlansContext';
import { useAuth } from '../state/AuthContext';
import type { HomeStackParamList } from '../navigation/types';

/** Tapping a recent-activity card has no destination in the design handoff
 * (unlike Gallery, which explicitly navigates to Plan Detail) — kept
 * read-only rather than guessing a target. */
export function HomeScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<HomeStackParamList>>();
  const { plans } = usePlans();
  const { user } = useAuth();
  // Falls back to the email's local part when no display name was given, so the
  // greeting never reads "Olá, ." for an account that skipped the name field.
  const firstName = (user?.name?.trim() || user?.username?.split('@')[0] || '').split(' ')[0];
  const recent = plans.slice(0, 4);

  return (
    <SafeAreaView className="flex-1 bg-cream" edges={['bottom']}>
      <ScrollView contentContainerClassName="px-6 py-7">
        <Text className="font-figtreeBold text-[12px] uppercase tracking-[0.14em] text-green-primary">Painel</Text>
        <Text className="mt-3 font-figtreeExtrabold text-[28px] tracking-[-0.02em] text-green-deep">
          {firstName ? `Olá, ${firstName}.` : 'Olá.'}
        </Text>
        <Text className="mt-2 font-figtree text-[15px] text-text-muted">
          Crie, escaneie e gerencie seus planos em um só lugar.
        </Text>

        <View className="mt-5 flex-row gap-3">
          <Pressable
            role="button"
            aria-label="Criar plano"
            onPress={() => navigation.navigate('CreatePlan')}
            className="flex-1 rounded-tl-[20px] rounded-tr-[6px] rounded-br-[20px] rounded-bl-[6px] bg-green-primary p-4"
          >
            <View className="h-[30px] w-[30px] items-center justify-center rounded-[9px] bg-white/15">
              <Text className="font-figtreeBold text-[18px] text-white">+</Text>
            </View>
            <Text className="mt-3 font-figtreeBold text-[15px] text-white">Criar plano</Text>
          </Pressable>
          <View className="flex-1 rounded-tl-[20px] rounded-tr-[6px] rounded-br-[20px] rounded-bl-[6px] border border-border bg-white p-4">
            <View className="h-[30px] w-[30px] items-center justify-center rounded-[9px] bg-mint-surface">
              <View className="h-3 w-3 rounded-[3px] border-2 border-green-primary" />
            </View>
            <Text className="mt-3 font-figtreeBold text-[15px] text-text-primary">Escanear</Text>
          </View>
        </View>

        <Text className="mt-7 font-figtreeExtrabold text-[18px] text-green-deep">Atividade recente</Text>
        <View className="mt-3.5 gap-2.5">
          {recent.map((plan) => (
            <Card key={plan.id} className="flex-row items-center gap-3.5 rounded-tl-[16px] rounded-tr-[5px] rounded-br-[16px] rounded-bl-[5px] p-3.5">
              <View className="h-10 w-10 items-center justify-center rounded-[11px] bg-mint-surface">
                <SunflowerMark size={20} />
              </View>
              <View className="min-w-0 flex-1">
                <Text className="font-figtreeBold text-[15px] text-text-primary">{plan.title}</Text>
                <Text className="mt-0.5 font-figtree text-[13px] text-text-muted">
                  Criado {plan.createdAt} · {plan.active ? 'ativo' : 'inativo'}
                </Text>
              </View>
            </Card>
          ))}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
