import { render, screen, userEvent } from '@testing-library/react-native';
import { Input } from './Input';

describe('Input', () => {
  it('renders its label and forwards text changes', async () => {
    const user = userEvent.setup();
    const onChangeText = jest.fn();
    await render(<Input label="Email" value="" onChangeText={onChangeText} />);
    expect(screen.getByText('Email')).toBeOnTheScreen();
    await user.type(screen.getByLabelText('Email'), 'voce@exemplo.com');
    expect(onChangeText).toHaveBeenCalled();
  });

  it('shows helper text when provided', async () => {
    await render(<Input label="Senha" value="" onChangeText={() => {}} helperText="Ao menos 8 caracteres." />);
    expect(screen.getByText('Ao menos 8 caracteres.')).toBeOnTheScreen();
  });

  it('shows error text instead of helper text when both are provided', async () => {
    await render(
      <Input
        label="Senha"
        value=""
        onChangeText={() => {}}
        helperText="Ao menos 8 caracteres."
        errorText="Campo obrigatório."
      />,
    );
    expect(screen.getByText('Campo obrigatório.')).toBeOnTheScreen();
    expect(screen.queryByText('Ao menos 8 caracteres.')).not.toBeOnTheScreen();
  });
});
