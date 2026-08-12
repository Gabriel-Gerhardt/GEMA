import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { EmptyState } from '../components/EmptyState';
import type { PublicStackParamList } from '../navigation/types';

export function NotFoundScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<PublicStackParamList>>();

  return (
    <SafeAreaView className="flex-1 bg-cream" edges={['top', 'bottom']}>
      <EmptyState
        className="flex-1"
        title="Página não encontrada"
        message="Não encontramos o que você procurava."
        actionLabel="Voltar ao início"
        onAction={() => navigation.navigate('Landing')}
      />
    </SafeAreaView>
  );
}
