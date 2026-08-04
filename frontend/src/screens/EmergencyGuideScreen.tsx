import { Linking, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRoute, type RouteProp } from '@react-navigation/native';
import { SunflowerMark } from '../components/SunflowerMark';
import { Button } from '../components/Button';
import { SectionReadItem } from '../components/SectionReadItem';
import { usePlans } from '../state/PlansContext';
import type { PublicStackParamList } from '../navigation/types';

/** Best-effort phone extraction from the emergency section's free-text
 * content — the Section model has no structured contact fields (out of
 * scope for this issue). Ceiling: if no digit run is found, the call
 * button silently does nothing rather than guessing. Upgrade path: add
 * structured contact fields to the plan/section data model. */
function extractPhoneDigits(text: string): string | null {
  const match = text.match(/[\d()+\-\s]{8,}/);
  if (!match) return null;
  const digits = match[0].replace(/\D/g, '');
  return digits.length >= 8 ? digits : null;
}

export function EmergencyGuideScreen() {
  const route = useRoute<RouteProp<PublicStackParamList, 'EmergencyGuide'>>();
  const { getPlanByPublicId } = usePlans();
  const plan = getPlanByPublicId(route.params.publicId);

  if (!plan) return null;

  const lastSection = plan.sections[plan.sections.length - 1];
  const isEmergencySection = Boolean(lastSection && /emerg/i.test(lastSection.title));
  const bodySections = isEmergencySection ? plan.sections.slice(0, -1) : plan.sections;
  const phoneDigits = isEmergencySection ? extractPhoneDigits(lastSection.content) : null;

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

        {isEmergencySection ? (
          <View className="mt-auto rounded-2xl border border-mint-border bg-mint-surface p-5">
            <Text className="font-figtreeBold text-[12px] uppercase tracking-[0.1em] text-gold-dark">
              Em uma emergência
            </Text>
            <Text className="mt-2 font-figtree text-[15px] leading-[1.4] text-text-primary">{lastSection.content}</Text>
            <Button
              onPress={() => phoneDigits && Linking.openURL(`tel:${phoneDigits}`)}
              disabled={!phoneDigits}
              className="mt-3.5 w-full"
            >
              Ligar agora
            </Button>
          </View>
        ) : null}
      </ScrollView>
    </SafeAreaView>
  );
}
