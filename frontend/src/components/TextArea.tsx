import { TextInput, type TextInputProps } from 'react-native';
import { colors } from '../theme/tokens';

interface TextAreaProps extends Pick<TextInputProps, 'value' | 'onChangeText' | 'placeholder'> {
  className?: string;
}

/** Multiline field for section content — the design's `.sarea` style. */
export function TextArea({ className = '', ...props }: TextAreaProps) {
  return (
    <TextInput
      multiline
      placeholderTextColor={colors.text.placeholder}
      className={`min-h-[64px] rounded-lg border border-border bg-cream px-3 py-2.5 font-figtree text-[13.5px] leading-[1.4] text-text-primary ${className}`}
      textAlignVertical="top"
      {...props}
    />
  );
}
