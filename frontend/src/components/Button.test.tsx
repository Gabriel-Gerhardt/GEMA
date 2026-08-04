import { render, screen, userEvent } from '@testing-library/react-native';
import { Button } from './Button';

describe('Button', () => {
  it('renders its label and responds to presses', async () => {
    const user = userEvent.setup();
    const onPress = jest.fn();
    await render(<Button onPress={onPress}>Criar plano</Button>);
    await user.press(screen.getByRole('button', { name: 'Criar plano' }));
    expect(onPress).toHaveBeenCalledTimes(1);
  });

  it('does not fire onPress when disabled', async () => {
    const user = userEvent.setup();
    const onPress = jest.fn();
    await render(
      <Button onPress={onPress} disabled>
        Entrar
      </Button>,
    );
    await user.press(screen.getByRole('button', { name: 'Entrar' }));
    expect(onPress).not.toHaveBeenCalled();
  });

  it('marks itself disabled via accessibility state', async () => {
    await render(
      <Button onPress={() => {}} disabled>
        Entrar
      </Button>,
    );
    expect(screen.getByRole('button', { name: 'Entrar' })).toBeDisabled();
  });
});
