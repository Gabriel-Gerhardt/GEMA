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
import * as api from '../api/endpoints';
import type { PublicStackParamList } from '../navigation/types';

export function ForgotPasswordScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<PublicStackParamList>>();
  const [email, setEmail] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [sent, setSent] = useState(false);

  async function handleSubmit() {
    if (!email.trim()) {
      setError('Informe seu email.');
      return;
    }
    setError(null);
    setIsSubmitting(true);
    try {
      await api.requestPasswordReset(email.trim());
      setSent(true);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Não foi possível enviar o email.');
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
          <Text className="mt-3 font-figtreeExtrabold text-[22px] text-green-deep">Recuperar acesso</Text>

          {sent ? (
            /* Worded so it says the same thing whether or not the address is
             * registered — the API answers identically on purpose, and the
             * screen must not undo that by being more specific than it is. */
            <>
              <Text className="mt-4 font-figtree text-[15px] leading-[1.5] text-text-muted">
                Se houver uma conta com esse email, enviamos um link para criar uma nova senha. O link vale por 30
                minutos.
              </Text>
              <Button onPress={() => navigation.navigate('Login')} variant="secondary" className="mt-5 w-full">
                Voltar para entrar
              </Button>
            </>
          ) : (
            <>
              <Text className="mt-2.5 font-figtree text-[14.5px] leading-[1.5] text-text-muted">
                Enviaremos um link para você criar uma nova senha.
              </Text>
              <View className="mt-5 gap-4">
                <Input
                  label="Email"
                  value={email}
                  onChangeText={setEmail}
                  placeholder="voce@exemplo.com"
                  autoCapitalize="none"
                  keyboardType="email-address"
                />
                <FormError message={error} />
                <Button onPress={handleSubmit} disabled={isSubmitting}>
                  {isSubmitting ? 'Enviando…' : 'Enviar link'}
                </Button>
              </View>
            </>
          )}
        </Card>
        {!sent ? (
          <Text
            role="link"
            aria-label="Voltar para entrar"
            onPress={() => navigation.navigate('Login')}
            className="mt-4.5 text-center font-figtreeSemibold text-[14px] text-green-deep underline"
          >
            Voltar para entrar
          </Text>
        ) : null}
      </ScrollView>
    </SafeAreaView>
  );
}
