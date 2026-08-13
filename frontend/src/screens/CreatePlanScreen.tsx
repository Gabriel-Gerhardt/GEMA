import { useState } from 'react';
import { Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Button } from '../components/Button';
import { FormError } from '../components/ScreenState';
import { SectionEditorItem } from '../components/SectionEditorItem';
import { usePlans } from '../state/PlansContext';
import { colors } from '../theme/tokens';
import type { HomeStackParamList } from '../navigation/types';

interface DraftSection {
  key: string;
  title: string;
  content: string;
}

let draftKeyCounter = 0;
function newDraftKey() {
  draftKeyCounter += 1;
  return `draft-${draftKeyCounter}`;
}

function sectionCountLabel(count: number) {
  return count === 1 ? '1 seção' : `${count} seções`;
}

export function CreatePlanScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<HomeStackParamList>>();
  const { createPlan } = usePlans();
  const [title, setTitle] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [sections, setSections] = useState<DraftSection[]>([
    { key: newDraftKey(), title: '', content: '' },
    { key: newDraftKey(), title: '', content: '' },
  ]);

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
    setSections((prev) => [...prev, { key: newDraftKey(), title: '', content: '' }]);
  }

  async function handleSubmit() {
    if (!title.trim()) {
      setError('Dê um título ao plano.');
      return;
    }
    // Empty blocks start on screen by design, so drop the untouched ones rather
    // than failing validation on rows the person never filled in.
    const filled = sections.filter((s) => s.title.trim() || s.content.trim());
    if (filled.some((s) => !s.title.trim() || !s.content.trim())) {
      setError('Preencha o título e o conteúdo de cada seção, ou deixe-a em branco.');
      return;
    }
    setError(null);
    setIsSubmitting(true);
    try {
      const created = await createPlan({
        title: title.trim(),
        sections: filled.map((s) => ({ title: s.title.trim(), content: s.content.trim() })),
      });
      navigation.navigate('PlanCreated', { planId: created.id });
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Não foi possível criar o plano.');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <SafeAreaView className="flex-1 bg-cream" edges={['bottom']}>
      <ScrollView contentContainerClassName="px-6 py-6" keyboardShouldPersistTaps="handled">
        <Text className="font-figtreeBold text-[12px] uppercase tracking-[0.14em] text-green-primary">Novo plano</Text>
        <Text className="mt-2.5 font-figtreeExtrabold text-[28px] tracking-[-0.02em] text-green-deep">
          Criar um plano
        </Text>
        <Text className="mt-2 font-figtree text-[14px] text-text-muted">
          Organize as informações em seções. Cada seção vira um bloco no guia que alguém vê ao escanear.
        </Text>

        <View className="mt-4.5 gap-2">
          <Text className="font-figtreeSemibold text-[13.5px] text-text-primary">Título do plano</Text>
          <TextInput
            value={title}
            onChangeText={setTitle}
            placeholder="Guia do Lucas"
            placeholderTextColor={colors.text.placeholder}
            className="rounded-xl border border-border bg-white px-3.5 py-3 font-figtree text-[15px] text-text-primary"
          />
        </View>

        <View className="mt-5 flex-row items-baseline justify-between">
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

        <View className="mt-4">
          <FormError message={error} />
        </View>
        <Button onPress={handleSubmit} disabled={isSubmitting} className="mt-4 w-full">
          {isSubmitting ? 'Criando…' : 'Criar plano'}
        </Button>
      </ScrollView>
    </SafeAreaView>
  );
}
