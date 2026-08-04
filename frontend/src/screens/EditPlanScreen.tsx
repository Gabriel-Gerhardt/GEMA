import { useState } from 'react';
import { Alert, Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation, useRoute, type RouteProp } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Button } from '../components/Button';
import { SectionEditorItem } from '../components/SectionEditorItem';
import { usePlans } from '../state/PlansContext';
import { colors } from '../theme/tokens';
import type { GalleryStackParamList } from '../navigation/types';

interface DraftSection {
  key: string;
  id?: string;
  title: string;
  content: string;
}

function sectionCountLabel(count: number) {
  return count === 1 ? '1 seção' : `${count} seções`;
}

export function EditPlanScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<GalleryStackParamList>>();
  const route = useRoute<RouteProp<GalleryStackParamList, 'EditPlan'>>();
  const { getPlan, updatePlan, deletePlan, toggleActive } = usePlans();
  const plan = getPlan(route.params.planId);

  const [title, setTitle] = useState(plan?.title ?? '');
  const [sections, setSections] = useState<DraftSection[]>(
    (plan?.sections ?? []).map((s) => ({ key: s.id, id: s.id, title: s.title, content: s.content })),
  );

  if (!plan) return null;

  function updateSection(key: string, patch: Partial<Pick<DraftSection, 'title' | 'content'>>) {
    setSections((prev) => prev.map((s) => (s.key === key ? { ...s, ...patch } : s)));
  }

  function removeSection(key: string) {
    setSections((prev) => prev.filter((s) => s.key !== key));
  }

  function moveSection(index: number, direction: -1 | 1) {
    setSections((prev) => {
      const next = [...prev];
      const target = index + direction;
      [next[index], next[target]] = [next[target], next[index]];
      return next;
    });
  }

  function addSection() {
    setSections((prev) => [...prev, { key: `new-${prev.length}-${Math.random().toString(36).slice(2, 6)}`, title: '', content: '' }]);
  }

  function handleSave() {
    updatePlan(
      plan!.id,
      title,
      sections.map((s) => ({ id: s.id, title: s.title, content: s.content })),
    );
    navigation.goBack();
  }

  function handleDelete() {
    Alert.alert('Excluir plano?', 'Essa ação não pode ser desfeita.', [
      { text: 'Cancelar', style: 'cancel' },
      {
        text: 'Excluir',
        style: 'destructive',
        onPress: () => {
          deletePlan(plan!.id);
          navigation.navigate('GalleryScreen');
        },
      },
    ]);
  }

  return (
    <SafeAreaView className="flex-1 bg-cream" edges={['bottom']}>
      <ScrollView contentContainerClassName="px-6 py-6" keyboardShouldPersistTaps="handled">
        <View className="flex-row items-center justify-between">
          <Text className="font-figtreeBold text-[12px] uppercase tracking-[0.14em] text-gold-dark">Editar plano</Text>
          <View className="flex-row items-center gap-2">
            <Text className="font-figtreeSemibold text-[12px] text-text-muted">{plan.active ? 'Ativo' : 'Inativo'}</Text>
            <Pressable
              role="switch"
              aria-checked={plan.active}
              onPress={() => toggleActive(plan!.id)}
              className={`h-6 w-10 justify-center rounded-full ${plan.active ? 'bg-green-primary' : 'bg-border'}`}
            >
              <View className={`h-[18px] w-[18px] rounded-full bg-white ${plan.active ? 'ml-[19px]' : 'ml-[3px]'}`} />
            </Pressable>
          </View>
        </View>

        <View className="mt-3 gap-1.5">
          <Text className="font-figtreeSemibold text-[13.5px] text-text-primary">Título do plano</Text>
          <TextInput
            value={title}
            onChangeText={setTitle}
            placeholderTextColor={colors.text.placeholder}
            className="rounded-xl border border-border bg-white px-3.5 py-3 font-figtreeSemibold text-[15px] text-text-primary"
          />
        </View>
        <Text className="mt-1.5 font-figtree text-[12px] text-text-muted">
          ID público: {plan.publicId} · gema.app/q/{plan.publicId}
        </Text>

        <View className="mt-4.5 flex-row items-baseline justify-between">
          <Text className="font-figtreeSemibold text-[12px] uppercase tracking-[0.06em] text-gold-dark">Seções</Text>
          <Text className="font-figtreeSemibold text-[12px] text-text-muted">{sectionCountLabel(sections.length)}</Text>
        </View>

        <View className="mt-2.5 gap-2.5">
          {sections.map((section, index) => (
            <SectionEditorItem
              key={section.key}
              index={index}
              title={section.title}
              content={section.content}
              onTitleChange={(value) => updateSection(section.key, { title: value })}
              onContentChange={(value) => updateSection(section.key, { content: value })}
              onRemove={() => removeSection(section.key)}
              onMoveUp={() => moveSection(index, -1)}
              onMoveDown={() => moveSection(index, 1)}
              canMoveUp={index > 0}
              canMoveDown={index < sections.length - 1}
            />
          ))}
          <Pressable
            role="button"
            aria-label="+ Adicionar seção"
            onPress={addSection}
            className="flex-row items-center justify-center gap-1.5 rounded-button border-[1.5px] border-dashed border-mint-border bg-mint-surface p-3.5"
          >
            <Text className="font-figtreeBold text-[17px] text-green-deep">+</Text>
            <Text className="font-figtreeBold text-[14px] text-green-deep">Adicionar seção</Text>
          </Pressable>
        </View>

        <Button onPress={handleSave} className="mt-3.5 w-full">
          Salvar alterações
        </Button>
        <View className="mt-3 flex-row justify-end">
          <Text role="link" aria-label="Excluir plano" onPress={handleDelete} className="font-figtreeSemibold text-[13px] text-danger">
            Excluir plano
          </Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
