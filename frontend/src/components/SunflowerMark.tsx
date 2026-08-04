import { Text, View } from 'react-native';
import Svg, { Circle, Ellipse, G } from 'react-native-svg';
import { colors } from '../theme/tokens';

const PETAL_COUNT = 12;

interface SunflowerMarkProps {
  size?: number;
}

/**
 * The Broto mark: 12 slim petals alternating between two greens, rotated
 * evenly around a gold-brown core (ring + center dot) — ported from the
 * design handoff's inline SVG (`<g id="gm">`).
 */
export function SunflowerMark({ size = 32 }: SunflowerMarkProps) {
  return (
    <Svg testID="sunflower-mark" width={size} height={size} viewBox="0 0 64 64" aria-label="GEMA sunflower mark">
      <G>
        {Array.from({ length: PETAL_COUNT }, (_, i) => (
          <Ellipse
            key={i}
            cx={32}
            cy={15}
            rx={3.6}
            ry={10.5}
            fill={i % 2 === 0 ? colors.green.mid : colors.green.midAlt}
            rotation={(360 / PETAL_COUNT) * i}
            origin="32, 32"
          />
        ))}
      </G>
      <Circle cx={32} cy={32} r={7.5} fill={colors.gold.DEFAULT} />
      <Circle cx={32} cy={32} r={3.6} fill={colors.gold.dark} />
    </Svg>
  );
}

interface SunflowerWordmarkProps {
  size?: number;
}

/** Mark + "GEMA" text lockup, used in headers. */
export function SunflowerWordmark({ size = 24 }: SunflowerWordmarkProps) {
  return (
    <View className="flex-row items-center gap-2">
      <SunflowerMark size={size} />
      <Text className="font-figtreeExtrabold text-[18px] text-text-primary tracking-wide">GEMA</Text>
    </View>
  );
}
