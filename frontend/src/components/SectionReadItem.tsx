import { Text, View } from 'react-native';

interface SectionReadItemProps {
  title: string;
  content: string;
}

/** Read-only section block for Plan Detail and the Emergency Guide View:
 * uppercase amber title label + plain body text. */
export function SectionReadItem({ title, content }: SectionReadItemProps) {
  return (
    <View className="gap-2 rounded-tl-[16px] rounded-tr-[5px] rounded-br-[16px] rounded-bl-[5px] border border-border bg-white p-3.5">
      <Text className="font-figtreeBold text-[10.5px] uppercase tracking-[0.1em] text-gold-dark">{title}</Text>
      <Text className="font-figtree text-[14px] leading-[1.4] text-text-primary">{content}</Text>
    </View>
  );
}
