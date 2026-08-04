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

export function LoginScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<PublicStackParamList>>();
  const { signIn } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  function handleSubmit() {
    if (!email.trim() || !password.trim()) return;
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
          <Text className="mt-3 font-figtreeExtrabold text-[22px] text-green-deep">Entrar</Text>
          <View className="mt-5 gap-4">
            <Input label="Email" value={email} onChangeText={setEmail} placeholder="voce@exemplo.com" autoCapitalize="none" keyboardType="email-address" />
            <Input label="Senha" value={password} onChangeText={setPassword} placeholder="••••••••" secureTextEntry />
            <Button onPress={handleSubmit}>Entrar</Button>
          </View>
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
