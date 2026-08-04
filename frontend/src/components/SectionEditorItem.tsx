import { Alert, Pressable, Text, TextInput, View } from 'react-native';
import { colors } from '../theme/tokens';
import { TextArea } from './TextArea';

interface SectionEditorItemProps {
  index: number;
  title: string;
  content: string;
  onTitleChange: (title: string) => void;
  onContentChange: (content: string) => void;
  onRemove: () => void;
  onMoveUp: () => void;
  onMoveDown: () => void;
  canMoveUp: boolean;
  canMoveDown: boolean;
  /** Set to false in tests to skip the native confirm dialog. */
  confirmBeforeRemove?: boolean;
}

/** One repeatable block in the Create/Edit Plan section editor: drag-handle
 * glyph (visual only, see DESIGN.md's noted reorder scoping), "Seção N"
 * label, × remove (confirmed via Alert), title/content fields, and
 * tap-to-move reorder controls. */
export function SectionEditorItem({
  index,
  title,
  content,
  onTitleChange,
  onContentChange,
  onRemove,
  onMoveUp,
  onMoveDown,
  canMoveUp,
  canMoveDown,
  confirmBeforeRemove = true,
}: SectionEditorItemProps) {
  function handleRemovePress() {
    if (!confirmBeforeRemove) {
      onRemove();
      return;
    }
    Alert.alert('Remover seção?', 'Essa ação não pode ser desfeita.', [
      { text: 'Cancelar', style: 'cancel' },
      { text: 'Remover', style: 'destructive', onPress: onRemove },
    ]);
  }

  return (
    <View className="gap-2.5 rounded-tl-[16px] rounded-tr-[5px] rounded-br-[16px] rounded-bl-[5px] border border-border bg-white p-3.5">
      <View className="flex-row items-center gap-2">
        <Text aria-hidden className="font-figtreeBold text-[13px] tracking-[-2px] text-text-placeholder">
          ⋮⋮
        </Text>
        <Text className="flex-1 font-figtreeBold text-[10.5px] uppercase tracking-[0.1em] text-gold-dark">
          Seção {index + 1}
        </Text>
        <Pressable role="button" aria-label="Mover para cima" aria-disabled={!canMoveUp} onPress={canMoveUp ? onMoveUp : undefined}>
          <Text className={`text-[16px] ${canMoveUp ? 'text-text-muted' : 'text-text-placeholder opacity-40'}`}>↑</Text>
        </Pressable>
        <Pressable role="button" aria-label="Mover para baixo" aria-disabled={!canMoveDown} onPress={canMoveDown ? onMoveDown : undefined}>
          <Text className={`text-[16px] ${canMoveDown ? 'text-text-muted' : 'text-text-placeholder opacity-40'}`}>↓</Text>
        </Pressable>
        <Pressable role="button" aria-label="Remover seção" onPress={handleRemovePress}>
          <Text className="text-[18px] text-text-placeholder">×</Text>
        </Pressable>
      </View>
      <TextInput
        value={title}
        onChangeText={onTitleChange}
        placeholder="Título — ex.: Sobre mim"
        placeholderTextColor={colors.text.placeholder}
        className="rounded-[9px] bg-cream px-2.5 py-2 font-figtreeSemibold text-[14px] text-text-primary"
      />
      <TextArea placeholder="Conteúdo…" value={content} onChangeText={onContentChange} />
    </View>
  );
}
