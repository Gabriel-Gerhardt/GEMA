import { View } from 'react-native';
import Svg, { Defs, Pattern, Path, Rect } from 'react-native-svg';
import { colors, radii } from '../theme/tokens';

interface QrPlaceholderProps {
  size?: number;
}

const STRIPE_SPACING = 8;

/**
 * Stand-in for a not-yet-generated QR code: a dashed box filled with the
 * repeating diagonal stripe from the design mock. The stripe is the part that
 * reads as "this is deliberately a placeholder" rather than a real, scannable
 * code — a plain empty box invites someone to try scanning it.
 *
 * Radius is the neutral, symmetric `qr` token, not the asymmetric card shape:
 * the placeholder is meant to read as unbranded chrome.
 */
export function QrPlaceholder({ size = 140 }: QrPlaceholderProps) {
  return (
    <View
      aria-label="espaço reservado para o código QR"
      style={{ width: size, height: size }}
      className="overflow-hidden rounded-qr border border-dashed border-mint-border bg-mint-surface"
    >
      <Svg testID="qr-placeholder-stripes" width="100%" height="100%">
        <Defs>
          <Pattern
            id="qr-stripes"
            patternUnits="userSpaceOnUse"
            width={STRIPE_SPACING}
            height={STRIPE_SPACING}
            patternTransform="rotate(45)"
          >
            <Path
              d={`M 0 0 L 0 ${STRIPE_SPACING}`}
              stroke={colors.mint.border}
              strokeWidth={3}
            />
          </Pattern>
        </Defs>
        <Rect x={0} y={0} width="100%" height="100%" fill="url(#qr-stripes)" rx={radii.qr} />
      </Svg>
    </View>
  );
}
