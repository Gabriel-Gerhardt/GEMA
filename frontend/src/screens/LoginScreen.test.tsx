import { render, screen, userEvent } from '@testing-library/react-native';
import { useNavigation } from '@react-navigation/native';
import { LoginScreen } from './LoginScreen';
import { useAuth } from '../state/AuthContext';

jest.mock('@react-navigation/native', () => ({
  ...jest.requireActual('@react-navigation/native'),
  useNavigation: jest.fn(),
}));
jest.mock('../state/AuthContext');

describe('LoginScreen', () => {
  const navigate = jest.fn();
  const signIn = jest.fn();
  beforeEach(() => {
    navigate.mockClear();
    signIn.mockClear();
    (useNavigation as jest.Mock).mockReturnValue({ navigate });
    (useAuth as jest.Mock).mockReturnValue({ signIn, isSignedIn: false });
  });

  it('renders the login form', async () => {
    await render(<LoginScreen />);
    expect(screen.getByRole('button', { name: 'Entrar' })).toBeOnTheScreen();
    expect(screen.getByLabelText('Email')).toBeOnTheScreen();
    expect(screen.getByLabelText('Senha')).toBeOnTheScreen();
  });

  it('signs in when submitted with both fields filled', async () => {
    const user = userEvent.setup();
    await render(<LoginScreen />);
    await user.type(screen.getByLabelText('Email'), 'voce@exemplo.com');
    await user.type(screen.getByLabelText('Senha'), 'segredo123');
    await user.press(screen.getByRole('button', { name: 'Entrar' }));
    expect(signIn).toHaveBeenCalledTimes(1);
  });

  it('does not sign in when a field is empty', async () => {
    const user = userEvent.setup();
    await render(<LoginScreen />);
    await user.press(screen.getByRole('button', { name: 'Entrar' }));
    expect(signIn).not.toHaveBeenCalled();
  });

  it('navigates to CreateAccount from the switch-account link', async () => {
    const user = userEvent.setup();
    await render(<LoginScreen />);
    await user.press(screen.getByRole('link', { name: 'Criar uma' }));
    expect(navigate).toHaveBeenCalledWith('CreateAccount');
  });
});
