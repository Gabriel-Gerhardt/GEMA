import { render, screen, userEvent, waitFor } from '@testing-library/react-native';
import { useNavigation, useRoute } from '@react-navigation/native';
import { ResetPasswordScreen } from './ResetPasswordScreen';
import * as api from '../api/endpoints';

jest.mock('@react-navigation/native', () => ({
  ...jest.requireActual('@react-navigation/native'),
  useNavigation: jest.fn(),
  useRoute: jest.fn(),
}));
jest.mock('../api/endpoints');

const mockApi = api as jest.Mocked<typeof api>;

async function fill(user: ReturnType<typeof userEvent.setup>, password: string, confirmation: string) {
  await user.type(screen.getByLabelText('Nova senha'), password);
  await user.type(screen.getByLabelText('Repita a nova senha'), confirmation);
  await user.press(screen.getByRole('button', { name: 'Salvar nova senha' }));
}

describe('ResetPasswordScreen', () => {
  const navigate = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    (useNavigation as jest.Mock).mockReturnValue({ navigate });
    (useRoute as jest.Mock).mockReturnValue({ params: { token: 'token-do-email' } });
    mockApi.confirmPasswordReset.mockResolvedValue(undefined);
  });

  it('sends the token from the link with the new password', async () => {
    const user = userEvent.setup();
    await render(<ResetPasswordScreen />);

    await fill(user, 'senha-nova-12345', 'senha-nova-12345');

    await waitFor(() =>
      expect(mockApi.confirmPasswordReset).toHaveBeenCalledWith('token-do-email', 'senha-nova-12345'),
    );
    expect(screen.getByText('Senha alterada. Agora é só entrar com ela.')).toBeOnTheScreen();
  });

  it('rejects a password shorter than the API allows, without a round trip', async () => {
    const user = userEvent.setup();
    await render(<ResetPasswordScreen />);

    await fill(user, 'curta7x', 'curta7x');

    expect(screen.getByText('A senha precisa ter ao menos 8 caracteres.')).toBeOnTheScreen();
    expect(mockApi.confirmPasswordReset).not.toHaveBeenCalled();
  });

  it('catches a mistyped confirmation before changing anything', async () => {
    const user = userEvent.setup();
    await render(<ResetPasswordScreen />);

    await fill(user, 'senha-nova-12345', 'senha-nova-54321');

    expect(screen.getByText('As senhas não coincidem.')).toBeOnTheScreen();
    expect(mockApi.confirmPasswordReset).not.toHaveBeenCalled();
  });

  it('tells the person to ask for a new link when this one is spent', async () => {
    // Unknown, expired and already-used are one case to the API and one case
    // here: get a fresh link.
    mockApi.confirmPasswordReset.mockRejectedValue(new Error('Invalid or expired password reset token'));
    const user = userEvent.setup();
    await render(<ResetPasswordScreen />);

    await fill(user, 'senha-nova-12345', 'senha-nova-12345');

    await waitFor(() => expect(screen.getByText('Invalid or expired password reset token')).toBeOnTheScreen());
  });

  it('handles being opened without a token at all', async () => {
    (useRoute as jest.Mock).mockReturnValue({ params: undefined });
    const user = userEvent.setup();
    await render(<ResetPasswordScreen />);

    await fill(user, 'senha-nova-12345', 'senha-nova-12345');

    expect(screen.getByText('Link inválido. Peça um novo email de recuperação.')).toBeOnTheScreen();
    expect(mockApi.confirmPasswordReset).not.toHaveBeenCalled();
  });
});
