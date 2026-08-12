import { View } from 'react-native';
import type { ReactNode } from 'react';
import { shadow } from '../theme/tokens';

interface CardProps {
  children: ReactNode;
  className?: string;
}

/**
 * The signature asymmetric card: sharp top-right/bottom-left corners, rounded
 * top-left/bottom-right — `border-radius: 24px 8px 24px 8px` in the design mock
 * — carrying DESIGN.md's soft green-tinted card shadow.
 *
 * The shadow is applied via `style` rather than a Tailwind class because
 * NativeWind's shadow utilities don't map onto React Native's separate
 * shadowColor/shadowOffset/elevation props with the tinted, sub-pixel values
 * this design uses.
 */
export function Card({ children, className = '' }: CardProps) {
  return (
    <View
      style={shadow.card}
      className={`rounded-tl-card rounded-tr-card-alt rounded-br-card rounded-bl-card-alt border border-border bg-white p-4 ${className}`}
    >
      {children}
    </View>
  );
}
