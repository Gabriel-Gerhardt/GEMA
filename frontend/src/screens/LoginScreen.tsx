import { useState } from 'react';
import { ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { SunflowerWordmark } from '../components/SunflowerMark';
import { Button } from '../components/Button';
import { Card } from '../components/Card';
import { Input } from '../components/Input';
import { FormError } from '../components/ScreenState';
import { useAuth } from '../state/AuthContext';
import type { PublicStackParamList } from '../navigation/types';

export function LoginScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<PublicStackParamList>>();
  const { signIn } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit() {
    if (!email.trim() || !password) {
      setError('Preencha email e senha.');
      return;
    }
    setError(null);
    setIsSubmitting(true);
    try {
      await signIn(email.trim(), password);
      // No navigation here: RootNavigator swaps to the tab shell as soon as the
      // session exists, so pushing a screen would fight it.
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Não foi possível entrar.');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <SafeAreaView className="flex-1 bg-cream" edges={['top', 'bottom']}>
      <View className="items-center border-b border-border bg-white px-5 py-4">
        <SunflowerWordmark />
      </View>
      <ScrollView contentContainerClassName="flex-grow justify-center px-7 py-6" keyboardShouldPersistTaps="handled">
        <Card className="p-7">
          <Text className="font-figtreeBold text-[12px] uppercase tracking-[0.14em] text-green-primary">Conta</Text>
          <Text className="mt-3 font-figtreeExtrabold text-[22px] text-green-deep">Entrar</Text>
          <View className="mt-5 gap-4">
            <Input
              label="Email"
              value={email}
              onChangeText={setEmail}
              placeholder="voce@exemplo.com"
              autoCapitalize="none"
              keyboardType="email-address"
            />
            <Input label="Senha" value={password} onChangeText={setPassword} placeholder="••••••••" secureTextEntry />
            <FormError message={error} />
            <Button onPress={handleSubmit} disabled={isSubmitting}>
              {isSubmitting ? 'Entrando…' : 'Entrar'}
            </Button>
          </View>
          <Text
            role="link"
            aria-label="Esqueci minha senha"
            onPress={() => navigation.navigate('ForgotPassword')}
            className="mt-4 self-center font-figtreeSemibold text-[13px] text-green-deep underline"
          >
            Esqueci minha senha
          </Text>
        </Card>
        <Text className="mt-4.5 text-center font-figtree text-[14px] text-text-muted">
          Não tem conta?{' '}
          <Text
            role="link"
            aria-label="Criar uma"
            onPress={() => navigation.navigate('CreateAccount')}
            className="font-figtreeSemibold text-green-deep underline"
          >
            Criar uma
          </Text>
        </Text>
      </ScrollView>
    </SafeAreaView>
  );
}
