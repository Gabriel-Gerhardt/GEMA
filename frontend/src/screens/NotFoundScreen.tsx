import { Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { SunflowerMark } from '../components/SunflowerMark';
import { Card } from '../components/Card';
import type { PublicStackParamList } from '../navigation/types';

export function NotFoundScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<PublicStackParamList>>();

  return (
    <SafeAreaView className="flex-1 bg-cream" edges={['top', 'bottom']}>
      <View className="flex-1 items-center justify-center px-7">
        <Card className="w-full items-center p-8.5">
          <SunflowerMark size={52} />
          <Text className="mt-4.5 font-figtreeExtrabold text-[22px] text-green-deep">Página não encontrada</Text>
          <Text className="mt-2.5 text-center font-figtree text-[15px] text-text-muted">
            Não encontramos o que você procurava.
          </Text>
          <Text
            role="link"
            aria-label="Voltar ao início"
            onPress={() => navigation.navigate('Landing')}
            className="mt-5.5 font-figtreeSemibold text-[14px] text-green-deep underline"
          >
            Voltar ao início
          </Text>
        </Card>
      </View>
    </SafeAreaView>
  );
}
