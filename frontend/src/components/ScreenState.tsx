import { ActivityIndicator, Text, View } from 'react-native';
import { EmptyState } from './EmptyState';
import { colors } from '../theme/tokens';

/** Centred spinner for a screen that has nothing to show yet. */
export function LoadingState({ label = 'Carregando…' }: { label?: string }) {
  return (
    <View aria-label={label} className="flex-1 items-center justify-center gap-3 px-7">
      <ActivityIndicator color={colors.green.primary} />
      <Text className="font-figtree text-[14px] text-text-muted">{label}</Text>
    </View>
  );
}

/**
 * A failed load, with a way to try again.
 *
 * Retry matters more here than in most apps: the person may be standing in a
 * pharmacy on bad signal trying to reach their own plan.
 */
export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <EmptyState
      className="flex-1"
      title="Não foi possível carregar"
      message={message}
      actionLabel={onRetry ? 'Tentar de novo' : undefined}
      onAction={onRetry}
    />
  );
}

/** Inline form error — a failed submit, shown next to the fields it concerns. */
export function FormError({ message }: { message: string | null }) {
  if (!message) return null;
  return (
    <View role="alert" className="rounded-input border border-danger/30 bg-danger/5 px-3.5 py-2.5">
      <Text className="font-figtree text-[13.5px] leading-[1.4] text-danger">{message}</Text>
    </View>
  );
}
