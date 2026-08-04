import { Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { SunflowerMark, SunflowerWordmark } from '../components/SunflowerMark';
import { Button } from '../components/Button';
import type { PublicStackParamList } from '../navigation/types';

const STEPS = [
  { number: '1', bold: 'Crie seu plano.', rest: ' Escreva, em seções, o que ajuda a te apoiar numa crise.' },
  { number: '2', bold: 'Leve com você.', rest: ' No crachá, na mochila ou na pulseira.' },
  { number: '3', bold: 'Alguém escaneia.', rest: ' Vê o guia na hora, sem precisar de conta.' },
];

export function LandingScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<PublicStackParamList>>();

  return (
    <SafeAreaView className="flex-1 bg-cream" edges={['top', 'bottom']}>
      <ScrollView contentContainerClassName="flex-grow px-6 pb-6 pt-4" keyboardShouldPersistTaps="handled">
        <View className="flex-row items-center justify-between">
          <SunflowerWordmark />
          <Pressable role="link" aria-label="Entrar" onPress={() => navigation.navigate('Login')}>
            <Text className="font-figtreeSemibold text-[14px] text-green-deep">Entrar</Text>
          </Pressable>
        </View>

        <View className="mt-8 items-center">
          <SunflowerMark size={88} />
          <Text className="mt-5 text-center font-figtreeExtrabold text-[29px] leading-[1.14] tracking-[-0.02em] text-green-deep">
            Um QR code que ajuda quando mais importa.
          </Text>
          <Text className="mt-3.5 max-w-[300px] text-center font-figtree text-[15px] leading-[1.55] text-text-muted">
            A GEMA cria planos pessoais com o que alguém precisa saber para apoiar você — ou quem você cuida — em uma
            crise.
          </Text>
          <Button onPress={() => navigation.navigate('Onboarding')} className="mt-5">
            Criar meu plano
          </Button>
          <Pressable role="link" aria-label="Já tenho uma conta" onPress={() => navigation.navigate('Login')} className="mt-3.5">
            <Text className="font-figtreeSemibold text-[14px] text-green-deep underline">Já tenho uma conta</Text>
          </Pressable>
        </View>

        <View className="mt-auto gap-4 border-t border-border pt-6">
          {STEPS.map((step) => (
            <View key={step.number} className="flex-row items-start gap-3">
              <Text className="w-4 font-figtreeExtrabold text-[15px] text-gold">{step.number}</Text>
              <Text className="flex-1 font-figtree text-[14px] leading-[1.45] text-text-primary">
                <Text className="font-figtreeBold">{step.bold}</Text>
                {step.rest}
              </Text>
            </View>
          ))}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
