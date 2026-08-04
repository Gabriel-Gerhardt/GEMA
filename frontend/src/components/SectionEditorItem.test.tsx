import { render, screen, userEvent } from '@testing-library/react-native';
import { SectionEditorItem } from './SectionEditorItem';

describe('SectionEditorItem', () => {
  const baseProps = {
    index: 0,
    title: '',
    content: '',
    onTitleChange: jest.fn(),
    onContentChange: jest.fn(),
    onRemove: jest.fn(),
    onMoveUp: jest.fn(),
    onMoveDown: jest.fn(),
    canMoveUp: false,
    canMoveDown: true,
  };

  afterEach(() => jest.clearAllMocks());

  it('labels itself by 1-based position', async () => {
    await render(<SectionEditorItem {...baseProps} index={1} />);
    expect(screen.getByText('Seção 2')).toBeOnTheScreen();
  });

  it('forwards title and content edits', async () => {
    const user = userEvent.setup();
    const onTitleChange = jest.fn();
    await render(<SectionEditorItem {...baseProps} onTitleChange={onTitleChange} />);
    await user.type(screen.getByPlaceholderText('Título — ex.: Sobre mim'), 'Sobre mim');
    expect(onTitleChange).toHaveBeenCalled();
  });

  it('calls onRemove when the × control is pressed and confirmed', async () => {
    const user = userEvent.setup();
    const onRemove = jest.fn();
    await render(<SectionEditorItem {...baseProps} onRemove={onRemove} confirmBeforeRemove={false} />);
    await user.press(screen.getByRole('button', { name: 'Remover seção' }));
    expect(onRemove).toHaveBeenCalledTimes(1);
  });

  it('disables move-up when canMoveUp is false and enables move-down', async () => {
    await render(<SectionEditorItem {...baseProps} />);
    expect(screen.getByRole('button', { name: 'Mover para cima' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Mover para baixo' })).toBeEnabled();
  });
});
