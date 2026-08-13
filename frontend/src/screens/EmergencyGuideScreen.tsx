import { Linking, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation, useRoute, type RouteProp } from '@react-navigation/native';
import { SunflowerMark } from '../components/SunflowerMark';
import { Button } from '../components/Button';
import { EmptyState } from '../components/EmptyState';
import { ErrorState, LoadingState } from '../components/ScreenState';
import { SectionReadItem } from '../components/SectionReadItem';
import { useAsyncResource } from '../hooks/useAsyncResource';
import * as api from '../api/endpoints';
import { ApiError } from '../api/client';
import type { PublicStackParamList } from '../navigation/types';

interface GuideSection {
  id: string;
  title: string;
  content: string;
}

interface Guide {
  ownerName: string | null;
  emergencyContactName: string | null;
  emergencyContactPhone: string | null;
  sections: GuideSection[];
}

/** Fallback phone extraction, for plans written before the structured contact
 * fields existed. Best-effort: with no digit run the call button is disabled
 * rather than guessing. */
function extractPhoneDigits(text: string): string | null {
  const match = text.match(/[\d()+\-\s]{8,}/);
  if (!match) return null;
  const digits = match[0].replace(/\D/g, '');
  return digits.length >= 8 ? digits : null;
}

/**
 * Locates the emergency-contact section anywhere in the list — not only last,
 * which used to silently cost the anchored panel and the call button for anyone
 * who ordered their sections differently.
 */
function findEmergencyIndex(sections: GuideSection[]): number {
  for (let i = sections.length - 1; i >= 0; i -= 1) {
    if (/emerg|urgen|socorro|contato/i.test(sections[i].title)) return i;
  }
  return -1;
}

export function EmergencyGuideScreen() {
  const navigation = useNavigation();
  const route = useRoute<RouteProp<PublicStackParamList, 'EmergencyGuide'>>();
  const publicId = route.params.publicId;

  // Deliberately the unauthenticated endpoints: whoever scanned this has no
  // account, and a deactivated plan answers 404 so the guide stops being shown.
  const { data, isLoading, error, errorCause, reload } = useAsyncResource<Guide>(async () => {
    const [plan, sections] = await Promise.all([
      api.getPublicPlan(publicId),
      api.getPublicSections(publicId),
    ]);
    return {
      ownerName: plan.ownerName,
      emergencyContactName: plan.emergencyContactName,
      emergencyContactPhone: plan.emergencyContactPhone,
      sections: sections.map((s) => ({ id: String(s.id), title: s.title, content: s.content })),
    };
  }, [publicId]);

  if (isLoading) {
    return (
      <SafeAreaView className="flex-1 bg-cream" edges={['top', 'bottom']}>
        <LoadingState label="Carregando guia…" />
      </SafeAreaView>
    );
  }

  // A 404 here means the plan is gone or deactivated; anything else is a
  // transport problem worth offering a retry for. Rendering nothing at all —
  // which this screen used to do — is the worst possible outcome on the one
  // surface a stranger reaches by scanning a code.
  if (error || !data) {
    const missing = errorCause instanceof ApiError && errorCause.status === 404;
    return (
      <SafeAreaView className="flex-1 bg-cream" edges={['top', 'bottom']}>
        {missing || !error ? (
          <EmptyState
            className="flex-1"
            title="Guia não encontrado"
            message="Este guia não existe ou não está mais disponível."
            actionLabel={navigation.canGoBack() ? 'Voltar' : undefined}
            onAction={navigation.canGoBack() ? () => navigation.goBack() : undefined}
          />
        ) : (
          <ErrorState message={error} onRetry={reload} />
        )}
      </SafeAreaView>
    );
  }

  const emergencyIndex = findEmergencyIndex(data.sections);
  const emergencySection = emergencyIndex >= 0 ? data.sections[emergencyIndex] : null;
  const bodySections = data.sections.filter((_, index) => index !== emergencyIndex);

  // The structured field wins; scanning prose is the legacy path. This is the
  // one action on the screen that has to work under pressure, so it must not
  // depend on how somebody happened to punctuate a sentence.
  const structuredPhone = data.emergencyContactPhone?.replace(/\D/g, '') || null;
  const phoneDigits =
    structuredPhone ?? (emergencySection ? extractPhoneDigits(emergencySection.content) : null);
  const hasContactPanel = Boolean(emergencySection || data.emergencyContactName || phoneDigits);

  const [introSection, ...remainingSections] = bodySections;
  const hasGreeting = Boolean(data.ownerName && introSection);
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
              Olá, meu nome é {data.ownerName}.
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

        {hasContactPanel ? (
          <View className="mt-auto rounded-card-sm border border-mint-border bg-mint-surface p-5">
            <Text className="font-figtreeBold text-[12px] uppercase tracking-[0.1em] text-gold-dark">
              {emergencySection?.title ?? 'Em uma emergência'}
            </Text>
            <Text className="mt-2 font-figtree text-[15px] leading-[1.4] text-text-primary">
              {data.emergencyContactName ?? emergencySection?.content}
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
