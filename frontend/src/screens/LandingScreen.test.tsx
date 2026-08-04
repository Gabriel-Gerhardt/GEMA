import { render, screen, userEvent } from '@testing-library/react-native';
import { useNavigation } from '@react-navigation/native';
import { LandingScreen } from './LandingScreen';

jest.mock('@react-navigation/native', () => ({
  ...jest.requireActual('@react-navigation/native'),
  useNavigation: jest.fn(),
}));

describe('LandingScreen', () => {
  const navigate = jest.fn();
  beforeEach(() => {
    navigate.mockClear();
    (useNavigation as jest.Mock).mockReturnValue({ navigate });
  });

  it('renders the hero headline and how-it-works steps', async () => {
    await render(<LandingScreen />);
    expect(screen.getByText('Um QR code que ajuda quando mais importa.')).toBeOnTheScreen();
    expect(screen.getByText(/Crie seu plano\./)).toBeOnTheScreen();
    expect(screen.getByText(/Leve com você\./)).toBeOnTheScreen();
    expect(screen.getByText(/Alguém escaneia\./)).toBeOnTheScreen();
  });

  it('navigates to Onboarding on the primary CTA', async () => {
    const user = userEvent.setup();
    await render(<LandingScreen />);
    await user.press(screen.getByRole('button', { name: 'Criar meu plano' }));
    expect(navigate).toHaveBeenCalledWith('Onboarding');
  });

  it('navigates to Login on the secondary link', async () => {
    const user = userEvent.setup();
    await render(<LandingScreen />);
    await user.press(screen.getByRole('link', { name: 'Já tenho uma conta' }));
    expect(navigate).toHaveBeenCalledWith('Login');
  });
});
