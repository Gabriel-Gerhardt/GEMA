import { Text, View } from 'react-native';
import { Card } from './Card';
import { SunflowerMark } from './SunflowerMark';

interface EmptyStateProps {
  title: string;
  message: string;
  /** Optional trailing link. Omit for a purely informational state. */
  actionLabel?: string;
  onAction?: () => void;
  markSize?: number;
  className?: string;
}

/**
 * The centered mark + message + link pattern DESIGN.md names in its component
 * inventory: "the centered icon + message + link pattern used by Not Found".
 *
 * It was previously hand-rolled inside NotFoundScreen, so every other
 * nothing-here moment (an empty gallery, a plan that no longer exists) had no
 * shared shape to reuse and simply rendered blank.
 */
export function EmptyState({
  title,
  message,
  actionLabel,
  onAction,
  markSize = 52,
  className = '',
}: EmptyStateProps) {
  return (
    <View className={`items-center justify-center px-7 ${className}`}>
      <Card className="w-full items-center p-8">
        <SunflowerMark size={markSize} />
        <Text className="mt-4.5 text-center font-figtreeExtrabold text-[22px] text-green-deep">{title}</Text>
        <Text className="mt-2.5 text-center font-figtree text-[15px] leading-[1.5] text-text-muted">{message}</Text>
        {actionLabel && onAction ? (
          <Text
            role="link"
            aria-label={actionLabel}
            onPress={onAction}
            className="mt-5.5 font-figtreeSemibold text-[14px] text-green-deep underline"
          >
            {actionLabel}
          </Text>
        ) : null}
      </Card>
    </View>
  );
}
