import { useState } from 'react';
import { ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { SunflowerMark, SunflowerWordmark } from '../components/SunflowerMark';
import { Button } from '../components/Button';
import { Card } from '../components/Card';
import { QrPlaceholder } from '../components/QrPlaceholder';
import type { PublicStackParamList } from '../navigation/types';

const TOTAL_STEPS = 3;

export function OnboardingScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<PublicStackParamList>>();
  const [step, setStep] = useState(1);

  return (
    <SafeAreaView className="flex-1 bg-cream" edges={['top', 'bottom']}>
      <View className="flex-row items-center justify-between border-b border-border bg-white px-5 py-4">
        <SunflowerWordmark />
        <View className="flex-row items-center gap-2.5">
          <Text className="font-figtreeSemibold text-[13px] text-text-muted">
            Passo {step} de {TOTAL_STEPS}
          </Text>
          <View className="flex-row gap-1.5">
            {Array.from({ length: TOTAL_STEPS }, (_, i) => (
              <View key={i} className={`h-[5px] w-[22px] rounded-full ${i < step ? 'bg-green-primary' : 'bg-border'}`} />
            ))}
          </View>
        </View>
      </View>

      <ScrollView contentContainerClassName="flex-grow px-7 pb-7 pt-9">
        {step === 1 ? (
          <View className="items-center">
            <SunflowerMark size={76} />
            <Text className="mt-5 text-center font-figtreeExtrabold text-[28px] leading-[1.15] tracking-[-0.02em] text-green-deep">
              Bem-vindo à GEMA
            </Text>
            <Text className="mt-3.5 max-w-[290px] text-center font-figtree text-[15px] leading-[1.5] text-text-muted">
              Nossa marca combina o cordão de girassol — símbolo usado para indicar uma deficiência oculta ou a
              necessidade de apoio — com o verde da tradição dos cordões de conscientização. Pétalas verdes, um miolo
              dourado e acolhedor: um sinal discreto de que ajuda pode ser pedida e oferecida.
            </Text>
            <View className="mt-auto w-full items-end">
              <Button onPress={() => setStep(2)}>Avançar</Button>
            </View>
          </View>
        ) : null}

        {step === 2 ? (
          <View>
            <Text className="font-figtreeExtrabold text-[44px] text-gold">1</Text>
            <Text className="mt-4 font-figtreeExtrabold text-[28px] leading-[1.15] tracking-[-0.02em] text-green-deep">
              Crie um plano
            </Text>
            <Text className="mt-3.5 font-figtree text-[15px] leading-[1.5] text-text-muted">
              Adicione um guia curto com o que alguém precisa saber ou fazer ao escanear o seu plano — em poucas
              palavras, com calma.
            </Text>
            <Card className="mt-5.5">
              <Text className="font-figtreeBold text-[12px] uppercase tracking-[0.14em] text-gold-dark">Exemplo</Text>
              <Text className="mt-2 font-figtreeBold text-[16px] text-text-primary">"Olá, meu nome é Lucas."</Text>
              <Text className="mt-1 font-figtree text-[14px] text-text-muted">
                Sou autista e posso ficar sobrecarregado…
              </Text>
            </Card>
            <View className="mt-auto flex-row justify-between">
              <Button variant="secondary" onPress={() => setStep(1)}>
                Voltar
              </Button>
              <Button onPress={() => setStep(3)}>Avançar</Button>
            </View>
          </View>
        ) : null}

        {step === 3 ? (
          <View className="flex-1">
            <Text className="font-figtreeExtrabold text-[44px] text-gold">2</Text>
            <Text className="mt-4 font-figtreeExtrabold text-[28px] leading-[1.15] tracking-[-0.02em] text-green-deep">
              Compartilhe
            </Text>
            <Text className="mt-3.5 font-figtree text-[15px] leading-[1.5] text-text-muted">
              Imprima, use ou prenda onde for mais útil — no crachá, na mochila, na pulseira. Quem escanear vê o guia
              na hora, sem precisar de conta.
            </Text>
            <View className="mt-6.5 items-center">
              <QrPlaceholder size={140} />
            </View>
            <View className="mt-auto flex-row items-center justify-between">
              <Button variant="secondary" onPress={() => setStep(2)}>
                Voltar
              </Button>
              <Button onPress={() => navigation.navigate('CreateAccount')}>Começar</Button>
            </View>
          </View>
        ) : null}
      </ScrollView>
    </SafeAreaView>
  );
}
