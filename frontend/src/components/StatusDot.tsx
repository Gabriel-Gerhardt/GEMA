import { View } from 'react-native';

interface StatusDotProps {
  active: boolean;
}

/** Small filled circle marking a plan active (green) or inactive (gray). */
export function StatusDot({ active }: StatusDotProps) {
  return (
    <View
      aria-label={active ? 'ativo' : 'inativo'}
      className={`h-2 w-2 rounded-full ${active ? 'bg-success' : 'bg-text-placeholder'}`}
    />
  );
}
