import { useState } from 'react';
import { Alert, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Button } from '../components/Button';
import { Input } from '../components/Input';
import { FormError, LoadingState } from '../components/ScreenState';
import * as api from '../api/endpoints';
import { useAuth } from '../state/AuthContext';

export function ProfileScreen() {
  const { user, signOut, refreshUser, onUnauthorized } = useAuth();
  const [isEditing, setIsEditing] = useState(false);
  const [name, setName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  if (!user) {
    return (
      <SafeAreaView className="flex-1 bg-cream" edges={['bottom']}>
        <LoadingState />
      </SafeAreaView>
    );
  }

  const displayName = user.name?.trim() || user.username;
  const initial = displayName.charAt(0).toUpperCase();

  function startEditing() {
    setName(user?.name ?? '');
    setError(null);
    setIsEditing(true);
  }

  async function handleSaveName() {
    setError(null);
    setIsSaving(true);
    try {
      await api.updateCurrentUser(name.trim(), { onUnauthorized });
      await refreshUser();
      setIsEditing(false);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Não foi possível salvar.');
    } finally {
      setIsSaving(false);
    }
  }

  function handleDeleteAccount() {
    Alert.alert(
      'Excluir conta?',
      'Todos os seus planos serão apagados junto. Essa ação não pode ser desfeita.',
      [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Excluir',
          style: 'destructive',
          onPress: async () => {
            try {
              await api.deleteCurrentUser({ onUnauthorized });
              await signOut();
            } catch (e) {
              setError(e instanceof Error ? e.message : 'Não foi possível excluir a conta.');
            }
          },
        },
      ],
    );
  }

  return (
    <SafeAreaView className="flex-1 bg-cream" edges={['bottom']}>
      <ScrollView contentContainerClassName="px-6 py-7">
        <View className="flex-row items-center gap-4">
          <View className="h-16 w-16 items-center justify-center rounded-tl-tile rounded-tr-tile-alt rounded-br-tile rounded-bl-tile-alt bg-mint-surface">
            <Text className="font-figtreeExtrabold text-[24px] text-green-deep">{initial}</Text>
          </View>
          <View className="min-w-0 flex-1">
            <Text className="font-figtreeExtrabold text-[22px] text-green-deep">{displayName}</Text>
            <Text className="mt-0.5 font-figtree text-[14px] text-text-muted">{user.username}</Text>
          </View>
        </View>

        <View className="mt-6 border-t border-border pt-5.5">
          <Text className="font-figtreeSemibold text-[13.5px] text-text-muted">Planos criados</Text>
          <Text className="mt-1.5 font-figtreeExtrabold text-[34px] text-text-primary">{user.planCount}</Text>
        </View>

        <View className="mt-6 gap-3.5 border-t border-border pt-5.5">
          {isEditing ? (
            <>
              <Input label="Nome" value={name} onChangeText={setName} placeholder="Como quer ser chamado" />
              <FormError message={error} />
              <View className="flex-row gap-3">
                <Button variant="secondary" onPress={() => setIsEditing(false)} className="flex-1">
                  Cancelar
                </Button>
                <Button onPress={handleSaveName} disabled={isSaving} className="flex-1">
                  {isSaving ? 'Salvando…' : 'Salvar'}
                </Button>
              </View>
            </>
          ) : (
            <>
              <FormError message={error} />
              <Button variant="secondary" onPress={startEditing} className="w-full">
                Editar perfil
              </Button>
              <Button variant="secondary" onPress={() => void signOut()} className="w-full">
                Sair
              </Button>
              <Text
                role="link"
                aria-label="Excluir conta"
                onPress={handleDeleteAccount}
                className="self-center font-figtreeSemibold text-[13px] text-danger"
              >
                Excluir conta
              </Text>
            </>
          )}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
