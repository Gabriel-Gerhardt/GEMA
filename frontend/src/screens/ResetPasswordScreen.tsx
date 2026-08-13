import { useState } from 'react';
import { ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation, useRoute, type RouteProp } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { SunflowerWordmark } from '../components/SunflowerMark';
import { Button } from '../components/Button';
import { Card } from '../components/Card';
import { Input } from '../components/Input';
import { FormError } from '../components/ScreenState';
import * as api from '../api/endpoints';
import type { PublicStackParamList } from '../navigation/types';

/** Reached from the emailed link, `/redefinir-senha?token=…`. */
export function ResetPasswordScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<PublicStackParamList>>();
  const route = useRoute<RouteProp<PublicStackParamList, 'ResetPassword'>>();
  const token = route.params?.token;

  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [done, setDone] = useState(false);

  async function handleSubmit() {
    if (!token) {
      setError('Link inválido. Peça um novo email de recuperação.');
      return;
    }
    if (password.length < 8) {
      setError('A senha precisa ter ao menos 8 caracteres.');
      return;
    }
    if (password !== confirmation) {
      setError('As senhas não coincidem.');
      return;
    }
    setError(null);
    setIsSubmitting(true);
    try {
      await api.confirmPasswordReset(token, password);
      setDone(true);
    } catch (e) {
      // The API does not distinguish unknown from expired from already-used, so
      // neither does this: all three mean "ask for a new link".
      setError(e instanceof Error ? e.message : 'Esse link não vale mais. Peça um novo.');
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
          <Text className="mt-3 font-figtreeExtrabold text-[22px] text-green-deep">Nova senha</Text>

          {done ? (
            <>
              <Text className="mt-4 font-figtree text-[15px] leading-[1.5] text-text-muted">
                Senha alterada. Agora é só entrar com ela.
              </Text>
              <Button onPress={() => navigation.navigate('Login')} className="mt-5 w-full">
                Entrar
              </Button>
            </>
          ) : (
            <View className="mt-5 gap-4">
              <Input
                label="Nova senha"
                value={password}
                onChangeText={setPassword}
                placeholder="••••••••"
                secureTextEntry
                helperText="Ao menos 8 caracteres."
              />
              <Input
                label="Repita a nova senha"
                value={confirmation}
                onChangeText={setConfirmation}
                placeholder="••••••••"
                secureTextEntry
              />
              <FormError message={error} />
              <Button onPress={handleSubmit} disabled={isSubmitting}>
                {isSubmitting ? 'Salvando…' : 'Salvar nova senha'}
              </Button>
            </View>
          )}
        </Card>
      </ScrollView>
    </SafeAreaView>
  );
}
