import { Linking, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation, useRoute, type RouteProp } from '@react-navigation/native';
import { SunflowerMark } from '../components/SunflowerMark';
import { Button } from '../components/Button';
import { EmptyState } from '../components/EmptyState';
import { SectionReadItem } from '../components/SectionReadItem';
import { usePlans } from '../state/PlansContext';
import type { Section } from '../state/types';
import type { PublicStackParamList } from '../navigation/types';

/** Best-effort phone extraction from the emergency section's free-text
 * content — the Section model has no structured contact fields (out of
 * scope for this issue). Ceiling: if no digit run is found, the call
 * button is disabled rather than guessing. Upgrade path: add structured
 * contact fields to the plan/section data model. */
function extractPhoneDigits(text: string): string | null {
  const match = text.match(/[\d()+\-\s]{8,}/);
  if (!match) return null;
  const digits = match[0].replace(/\D/g, '');
  return digits.length >= 8 ? digits : null;
}

/**
 * Locates the emergency-contact section anywhere in the list.
 *
 * Previously this only recognised the section when it happened to be the very
 * last one, so an owner who wrote their sections in a different order lost the
 * anchored contact panel and the "Ligar agora" button entirely — silently, and
 * exactly in the moment the screen exists for.
 */
function findEmergencyIndex(sections: Section[]): number {
  for (let i = sections.length - 1; i >= 0; i -= 1) {
    if (/emerg|urgen|socorro|contato/i.test(sections[i].title)) return i;
  }
  return -1;
}

export function EmergencyGuideScreen() {
  const navigation = useNavigation();
  const route = useRoute<RouteProp<PublicStackParamList, 'EmergencyGuide'>>();
  const { getPlanByPublicId } = usePlans();
  const plan = getPlanByPublicId(route.params.publicId);

  // A missing — or deactivated — plan used to render `null`, i.e. a blank white
  // screen, on the one surface a stranger reaches by scanning a code. The API
  // treats a deactivated plan as absent (GET /api/q/{id} → 404), and this
  // mirrors that: the Ativo/Inativo toggle only means something if the guide
  // actually stops being shown.
  if (!plan || !plan.active) {
    return (
      <SafeAreaView className="flex-1 bg-cream" edges={['top', 'bottom']}>
        <EmptyState
          className="flex-1"
          title="Guia não encontrado"
          message="Este guia não existe ou não está mais disponível."
          actionLabel={navigation.canGoBack() ? 'Voltar' : undefined}
          onAction={navigation.canGoBack() ? () => navigation.goBack() : undefined}
        />
      </SafeAreaView>
    );
  }

  const emergencyIndex = findEmergencyIndex(plan.sections);
  const emergencySection = emergencyIndex >= 0 ? plan.sections[emergencyIndex] : null;
  const bodySections = plan.sections.filter((_, index) => index !== emergencyIndex);
  const phoneDigits = emergencySection ? extractPhoneDigits(emergencySection.content) : null;

  // The design's greeting headline ("Olá, meu nome é Lucas.") + framing
  // paragraph is the first section rendered as plain text, not as a
  // labeled SectionReadItem card — only when the plan has an ownerName to
  // greet with (see the `ownerName` field's doc comment in state/types.ts).
  const [introSection, ...remainingSections] = bodySections;
  const hasGreeting = Boolean(plan.ownerName && introSection);
  const readSections = hasGreeting ? remainingSections : bodySections;

  return (
    <SafeAreaView className="flex-1 bg-cream" edges={['top', 'bottom']}>
      <View className="h-1.5 bg-gold" />
      <ScrollView contentContainerClassName="flex-grow px-7 py-7">
        <View className="flex-row items-center gap-2">
          <SunflowerMark size={22} />
          <Text className="font-figtreeBold text-[12px] uppercase tracking-[0.14em] text-green-primary">
            Guia de apoio
          </Text>
        </View>

        {hasGreeting ? (
          <>
            <Text className="mt-5 font-figtreeExtrabold text-[30px] leading-[1.15] tracking-[-0.02em] text-green-deep">
              Olá, meu nome é {plan.ownerName}.
            </Text>
            <Text className="mt-4 font-figtree text-[16px] leading-[1.55] text-text-primary">
              {introSection.content}
            </Text>
          </>
        ) : null}

        <View className="mt-5 gap-3">
          {readSections.map((section) => (
            <SectionReadItem key={section.id} title={section.title} content={section.content} />
          ))}
        </View>

        {emergencySection ? (
          <View className="mt-auto rounded-card-sm border border-mint-border bg-mint-surface p-5">
            <Text className="font-figtreeBold text-[12px] uppercase tracking-[0.1em] text-gold-dark">
              {emergencySection.title}
            </Text>
            <Text className="mt-2 font-figtree text-[15px] leading-[1.4] text-text-primary">
              {emergencySection.content}
            </Text>
            <Button
              onPress={() => phoneDigits && Linking.openURL(`tel:${phoneDigits}`)}
              disabled={!phoneDigits}
              className="mt-3.5 w-full rounded-button-sm"
            >
              Ligar agora
            </Button>
          </View>
        ) : null}
      </ScrollView>
    </SafeAreaView>
  );
}
