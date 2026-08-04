import { Pressable, Text } from 'react-native';
import type { ReactNode } from 'react';

export type ButtonVariant = 'primary' | 'secondary';

interface ButtonProps {
  children: ReactNode;
  onPress: () => void;
  variant?: ButtonVariant;
  disabled?: boolean;
  className?: string;
}

const CONTAINER_VARIANT_CLASSES: Record<ButtonVariant, string> = {
  primary: 'bg-green-primary',
  secondary: 'bg-white border border-border',
};

const TEXT_VARIANT_CLASSES: Record<ButtonVariant, string> = {
  primary: 'text-white',
  secondary: 'text-text-primary',
};

export function Button({ children, onPress, variant = 'primary', disabled, className = '' }: ButtonProps) {
  return (
    <Pressable
      role="button"
      aria-disabled={Boolean(disabled)}
      onPress={disabled ? undefined : onPress}
      className={`rounded-button items-center justify-center px-6 py-3.5 ${CONTAINER_VARIANT_CLASSES[variant]} ${disabled ? 'opacity-50' : ''} ${className}`}
    >
      <Text className={`font-figtreeSemibold text-[16px] ${TEXT_VARIANT_CLASSES[variant]}`}>{children}</Text>
    </Pressable>
  );
}
