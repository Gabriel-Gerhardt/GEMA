import { render, screen, userEvent, waitFor } from '@testing-library/react-native';
import { useNavigation } from '@react-navigation/native';
import { ForgotPasswordScreen } from './ForgotPasswordScreen';
import * as api from '../api/endpoints';

jest.mock('@react-navigation/native', () => ({
  ...jest.requireActual('@react-navigation/native'),
  useNavigation: jest.fn(),
}));
jest.mock('../api/endpoints');

const mockApi = api as jest.Mocked<typeof api>;

describe('ForgotPasswordScreen', () => {
  const navigate = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    (useNavigation as jest.Mock).mockReturnValue({ navigate });
    mockApi.requestPasswordReset.mockResolvedValue(undefined);
  });

  it('requests a reset link for the address given', async () => {
    const user = userEvent.setup();
    await render(<ForgotPasswordScreen />);

    await user.type(screen.getByLabelText('Email'), 'alice@exemplo.com');
    await user.press(screen.getByRole('button', { name: 'Enviar link' }));

    await waitFor(() => expect(mockApi.requestPasswordReset).toHaveBeenCalledWith('alice@exemplo.com'));
  });

  it('confirms without saying whether the account exists', async () => {
    // The API answers identically either way on purpose; the wording here must
    // not undo that by being more specific than the server is.
    const user = userEvent.setup();
    await render(<ForgotPasswordScreen />);

    await user.type(screen.getByLabelText('Email'), 'fantasma@exemplo.com');
    await user.press(screen.getByRole('button', { name: 'Enviar link' }));

    await waitFor(() => expect(screen.getByText(/Se houver uma conta com esse email/)).toBeOnTheScreen());
  });

  it('asks for an address before calling the API', async () => {
    const user = userEvent.setup();
    await render(<ForgotPasswordScreen />);

    await user.press(screen.getByRole('button', { name: 'Enviar link' }));

    expect(screen.getByText('Informe seu email.')).toBeOnTheScreen();
    expect(mockApi.requestPasswordReset).not.toHaveBeenCalled();
  });

  it('shows a failure instead of pretending the email went out', async () => {
    mockApi.requestPasswordReset.mockRejectedValue(new Error('Não foi possível falar com o servidor.'));
    const user = userEvent.setup();
    await render(<ForgotPasswordScreen />);

    await user.type(screen.getByLabelText('Email'), 'alice@exemplo.com');
    await user.press(screen.getByRole('button', { name: 'Enviar link' }));

    await waitFor(() => expect(screen.getByText('Não foi possível falar com o servidor.')).toBeOnTheScreen());
  });
});
