import { View } from 'react-native';
import type { ReactNode } from 'react';

interface CardProps {
  children: ReactNode;
  className?: string;
}

/** The signature asymmetric card: sharp top-right/bottom-left corners,
 * rounded top-left/bottom-right — `border-radius: 24px 8px 24px 8px` in the
 * design mock. */
export function Card({ children, className = '' }: CardProps) {
  return (
    <View
      className={`rounded-tl-[24px] rounded-tr-[8px] rounded-br-[24px] rounded-bl-[8px] border border-border bg-white p-4 ${className}`}
    >
      {children}
    </View>
  );
}
