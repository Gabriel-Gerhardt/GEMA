import { useState } from 'react';
import { ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { SunflowerWordmark } from '../components/SunflowerMark';
import { Button } from '../components/Button';
import { Card } from '../components/Card';
import { Input } from '../components/Input';
import { useAuth } from '../state/AuthContext';
import type { PublicStackParamList } from '../navigation/types';

export function CreateAccountScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<PublicStackParamList>>();
  const { signIn } = useAuth();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  function handleSubmit() {
    if (!name.trim() || !email.trim() || !password.trim()) return;
    signIn();
  }

  return (
    <SafeAreaView className="flex-1 bg-cream" edges={['top', 'bottom']}>
      <View className="items-center border-b border-border bg-white px-5 py-4">
        <SunflowerWordmark />
      </View>
      <ScrollView contentContainerClassName="flex-grow justify-center px-7 py-6" keyboardShouldPersistTaps="handled">
        <Card className="p-7">
          <Text className="font-figtreeBold text-[12px] uppercase tracking-[0.14em] text-green-primary">Conta</Text>
          <Text className="mt-3 font-figtreeExtrabold text-[22px] text-green-deep">Crie sua conta</Text>
          <View className="mt-5 gap-4">
            <Input label="Nome" value={name} onChangeText={setName} placeholder="Eduarda Souza" />
            <Input label="Email" value={email} onChangeText={setEmail} placeholder="voce@exemplo.com" autoCapitalize="none" keyboardType="email-address" />
            <Input
              label="Senha"
              value={password}
              onChangeText={setPassword}
              placeholder="••••••••"
              secureTextEntry
              helperText="Ao menos 8 caracteres."
            />
            <Button onPress={handleSubmit}>Criar conta</Button>
          </View>
        </Card>
        <Text className="mt-4 text-center font-figtree text-[14px] leading-[1.6] text-text-muted">
          Já tem conta?{' '}
          <Text role="link" aria-label="Entrar" onPress={() => navigation.navigate('Login')} className="font-figtreeSemibold text-green-deep underline">
            Entrar
          </Text>
          {'\n'}Novo por aqui?{' '}
          <Text
            role="link"
            aria-label="Veja como funciona"
            onPress={() => navigation.navigate('Onboarding')}
            className="font-figtreeSemibold text-green-deep underline"
          >
            Veja como funciona
          </Text>
        </Text>
      </ScrollView>
    </SafeAreaView>
  );
}
