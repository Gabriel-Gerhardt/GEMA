import { Text, TextInput, View, type TextInputProps } from 'react-native';
import { colors } from '../theme/tokens';

interface InputProps extends Pick<TextInputProps, 'value' | 'onChangeText' | 'placeholder' | 'secureTextEntry' | 'autoCapitalize' | 'keyboardType'> {
  label: string;
  helperText?: string;
  errorText?: string;
}

export function Input({ label, helperText, errorText, ...inputProps }: InputProps) {
  const hasError = Boolean(errorText);
  return (
    <View className="gap-1.5">
      <Text className="font-figtreeSemibold text-[13.5px] text-text-primary">{label}</Text>
      <TextInput
        aria-label={label}
        placeholderTextColor={colors.text.placeholder}
        className={`rounded-xl border bg-white px-3.5 py-3 font-figtree text-[15px] text-text-primary ${hasError ? 'border-danger' : 'border-border'}`}
        {...inputProps}
      />
      {hasError ? (
        <Text className="font-figtree text-[13px] text-danger">{errorText}</Text>
      ) : helperText ? (
        <Text className="font-figtree text-[13px] text-text-muted">{helperText}</Text>
      ) : null}
    </View>
  );
}
