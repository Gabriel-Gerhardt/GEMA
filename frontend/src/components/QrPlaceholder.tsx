import { Text, View } from 'react-native';

interface QrPlaceholderProps {
  size?: number;
}

/** Dashed placeholder box standing in for a generated QR code — no real QR
 * generation library is in scope for this issue (no backend integration). */
export function QrPlaceholder({ size = 140 }: QrPlaceholderProps) {
  return (
    <View
      aria-label="espaço reservado para o código QR"
      style={{ width: size, height: size }}
      className="items-center justify-center rounded-2xl border border-dashed border-mint-border bg-mint-surface"
    >
      <Text className="font-figtreeMedium text-[11px] text-green-primary">código QR</Text>
    </View>
  );
}
