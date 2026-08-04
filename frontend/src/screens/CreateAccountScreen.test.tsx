import { render, screen, userEvent } from '@testing-library/react-native';
import { useNavigation } from '@react-navigation/native';
import { CreateAccountScreen } from './CreateAccountScreen';
import { useAuth } from '../state/AuthContext';

jest.mock('@react-navigation/native', () => ({
  ...jest.requireActual('@react-navigation/native'),
  useNavigation: jest.fn(),
}));
jest.mock('../state/AuthContext');

describe('CreateAccountScreen', () => {
  const navigate = jest.fn();
  const signIn = jest.fn();
  beforeEach(() => {
    navigate.mockClear();
    signIn.mockClear();
    (useNavigation as jest.Mock).mockReturnValue({ navigate });
    (useAuth as jest.Mock).mockReturnValue({ signIn, isSignedIn: false });
  });

  it('renders the create-account form with a helper text on password', async () => {
    await render(<CreateAccountScreen />);
    expect(screen.getByLabelText('Nome')).toBeOnTheScreen();
    expect(screen.getByLabelText('Email')).toBeOnTheScreen();
    expect(screen.getByLabelText('Senha')).toBeOnTheScreen();
    expect(screen.getByText('Ao menos 8 caracteres.')).toBeOnTheScreen();
  });

  it('signs in when submitted with all fields filled', async () => {
    const user = userEvent.setup();
    await render(<CreateAccountScreen />);
    await user.type(screen.getByLabelText('Nome'), 'Eduarda Souza');
    await user.type(screen.getByLabelText('Email'), 'eduarda@exemplo.com');
    await user.type(screen.getByLabelText('Senha'), 'segredo123');
    await user.press(screen.getByRole('button', { name: 'Criar conta' }));
    expect(signIn).toHaveBeenCalledTimes(1);
  });

  it('navigates to Login and Onboarding from the footer links', async () => {
    const user = userEvent.setup();
    await render(<CreateAccountScreen />);
    await user.press(screen.getByRole('link', { name: 'Entrar' }));
    expect(navigate).toHaveBeenCalledWith('Login');
    await user.press(screen.getByRole('link', { name: 'Veja como funciona' }));
    expect(navigate).toHaveBeenCalledWith('Onboarding');
  });
});
