import { render, screen, userEvent } from '@testing-library/react-native';
import { TextArea } from './TextArea';

describe('TextArea', () => {
  it('renders as a multiline input and forwards text changes', async () => {
    const user = userEvent.setup();
    const onChangeText = jest.fn();
    await render(<TextArea placeholder="Conteúdo" value="" onChangeText={onChangeText} />);
    const field = screen.getByPlaceholderText('Conteúdo');
    expect(field.props.multiline).toBe(true);
    await user.type(field, 'Sou autista.');
    expect(onChangeText).toHaveBeenCalled();
  });
});
