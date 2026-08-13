import { useEffect, useState } from 'react';
import { Alert, Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation, useRoute, type RouteProp } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Button } from '../components/Button';
import { EmptyState } from '../components/EmptyState';
import { ErrorState, FormError, LoadingState } from '../components/ScreenState';
import { SectionEditorItem } from '../components/SectionEditorItem';
import { useAsyncResource } from '../hooks/useAsyncResource';
import { usePlans } from '../state/PlansContext';
import { colors } from '../theme/tokens';
import type { GalleryStackParamList } from '../navigation/types';

interface DraftSection {
  key: string;
  title: string;
  content: string;
}

function sectionCountLabel(count: number) {
  return count === 1 ? '1 seção' : `${count} seções`;
}

export function EditPlanScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<GalleryStackParamList>>();
  const route = useRoute<RouteProp<GalleryStackParamList, 'EditPlan'>>();
  const { fetchPlan, updatePlan, deletePlan } = usePlans();
  const planId = route.params.planId;

  const { data: plan, isLoading, error, reload } = useAsyncResource(() => fetchPlan(planId), [planId]);

  const [title, setTitle] = useState('');
  const [active, setActive] = useState(true);
  const [sections, setSections] = useState<DraftSection[]>([]);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  // Seed the form once the plan arrives. The draft is local from then on, so
  // typing is never interrupted by a background refresh.
  useEffect(() => {
    if (!plan) return;
    setTitle(plan.title);
    setActive(plan.active);
    setSections(plan.sections.map((s) => ({ key: s.id, title: s.title, content: s.content })));
  }, [plan]);

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
    setSections((prev) => [
      ...prev,
      { key: `new-${prev.length}-${Math.random().toString(36).slice(2, 6)}`, title: '', content: '' },
    ]);
  }

  async function handleSave() {
    if (!title.trim()) {
      setSubmitError('Dê um título ao plano.');
      return;
    }
    // The API rejects a blank section title or body, so say so here rather than
    // bouncing off a 400 the person has to decode.
    if (sections.some((s) => !s.title.trim() || !s.content.trim())) {
      setSubmitError('Preencha o título e o conteúdo de cada seção, ou remova as vazias.');
      return;
    }
    setSubmitError(null);
    setIsSaving(true);
    try {
      await updatePlan(planId, {
        title: title.trim(),
        active,
        sections: sections.map((s) => ({ title: s.title.trim(), content: s.content.trim() })),
      });
      navigation.goBack();
    } catch (e) {
      setSubmitError(e instanceof Error ? e.message : 'Não foi possível salvar.');
    } finally {
      setIsSaving(false);
    }
  }

  function handleDelete() {
    Alert.alert('Excluir plano?', 'Essa ação não pode ser desfeita.', [
      { text: 'Cancelar', style: 'cancel' },
      {
        text: 'Excluir',
        style: 'destructive',
        onPress: async () => {
          try {
            await deletePlan(planId);
            navigation.navigate('GalleryScreen');
          } catch (e) {
            setSubmitError(e instanceof Error ? e.message : 'Não foi possível excluir.');
          }
        },
      },
    ]);
  }

  if (isLoading && !plan) {
    return (
      <SafeAreaView className="flex-1 bg-cream" edges={['bottom']}>
        <LoadingState label="Carregando plano…" />
      </SafeAreaView>
    );
  }

  if (error && !plan) {
    return (
      <SafeAreaView className="flex-1 bg-cream" edges={['bottom']}>
        <ErrorState message={error} onRetry={reload} />
      </SafeAreaView>
    );
  }

  if (!plan) {
    return (
      <SafeAreaView className="flex-1 bg-cream" edges={['bottom']}>
        <EmptyState
          className="flex-1"
          title="Plano não encontrado"
          message="Este plano foi excluído ou não existe mais."
          actionLabel="Voltar à galeria"
          onAction={() => navigation.navigate('GalleryScreen')}
        />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView className="flex-1 bg-cream" edges={['bottom']}>
      <ScrollView contentContainerClassName="px-6 py-6" keyboardShouldPersistTaps="handled">
        <View className="flex-row items-center justify-between">
          <Text className="font-figtreeBold text-[12px] uppercase tracking-[0.14em] text-gold-dark">Editar plano</Text>
          <View className="flex-row items-center gap-2">
            <Text className="font-figtreeSemibold text-[12px] text-text-muted">{active ? 'Ativo' : 'Inativo'}</Text>
            <Pressable
              role="switch"
              aria-checked={active}
              aria-label="Plano ativo"
              onPress={() => setActive((current) => !current)}
              className={`h-6 w-10 justify-center rounded-full ${active ? 'bg-green-primary' : 'bg-border'}`}
            >
              <View className={`h-[18px] w-[18px] rounded-full bg-white ${active ? 'ml-[19px]' : 'ml-[3px]'}`} />
            </Pressable>
          </View>
        </View>

        <View className="mt-3 gap-1.5">
          <Text className="font-figtreeSemibold text-[13.5px] text-text-primary">Título do plano</Text>
          <TextInput
            value={title}
            onChangeText={setTitle}
            placeholderTextColor={colors.text.placeholder}
            className="rounded-input border border-border bg-white px-3.5 py-3 font-figtreeSemibold text-[15px] text-text-primary"
          />
        </View>
        <Text className="mt-1.5 font-figtree text-[12px] text-text-muted">ID público: {plan.publicId}</Text>

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

        <View className="mt-3.5">
          <FormError message={submitError} />
        </View>
        <Button onPress={handleSave} disabled={isSaving} className="mt-3.5 w-full">
          {isSaving ? 'Salvando…' : 'Salvar alterações'}
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
