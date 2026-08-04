import { render, screen, userEvent } from '@testing-library/react-native';
import { useNavigation } from '@react-navigation/native';
import { OnboardingScreen } from './OnboardingScreen';

jest.mock('@react-navigation/native', () => ({
  ...jest.requireActual('@react-navigation/native'),
  useNavigation: jest.fn(),
}));

describe('OnboardingScreen', () => {
  const navigate = jest.fn();
  beforeEach(() => {
    navigate.mockClear();
    (useNavigation as jest.Mock).mockReturnValue({ navigate });
  });

  it('starts on step 1 of 3', async () => {
    await render(<OnboardingScreen />);
    expect(screen.getByText('Passo 1 de 3')).toBeOnTheScreen();
    expect(screen.getByText('Bem-vindo à GEMA')).toBeOnTheScreen();
  });

  it('advances through all 3 steps and navigates to CreateAccount on the last', async () => {
    const user = userEvent.setup();
    await render(<OnboardingScreen />);

    await user.press(screen.getByRole('button', { name: 'Avançar' }));
    expect(screen.getByText('Passo 2 de 3')).toBeOnTheScreen();
    expect(screen.getByText('Crie um plano')).toBeOnTheScreen();

    await user.press(screen.getByRole('button', { name: 'Avançar' }));
    expect(screen.getByText('Passo 3 de 3')).toBeOnTheScreen();
    expect(screen.getByText('Compartilhe')).toBeOnTheScreen();

    await user.press(screen.getByRole('button', { name: 'Começar' }));
    expect(navigate).toHaveBeenCalledWith('CreateAccount');
  });

  it('goes back a step from step 2', async () => {
    const user = userEvent.setup();
    await render(<OnboardingScreen />);
    await user.press(screen.getByRole('button', { name: 'Avançar' }));
    await user.press(screen.getByRole('button', { name: 'Voltar' }));
    expect(screen.getByText('Passo 1 de 3')).toBeOnTheScreen();
  });
});
