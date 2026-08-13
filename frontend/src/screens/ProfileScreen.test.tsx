import { render, screen, userEvent, waitFor } from '@testing-library/react-native';
import { Alert } from 'react-native';
import { ProfileScreen } from './ProfileScreen';
import { useAuth } from '../state/AuthContext';
import * as api from '../api/endpoints';

jest.mock('../state/AuthContext');
jest.mock('../api/endpoints');

describe('ProfileScreen', () => {
  const signOut = jest.fn();
  beforeEach(() => {
    signOut.mockClear();
    (useAuth as jest.Mock).mockReturnValue({
      signOut,
      user: { id: 1, username: 'eduarda.souza@exemplo.com', name: 'Eduarda Souza', role: 'USER', planCount: 3 },
      refreshUser: jest.fn(),
      onUnauthorized: jest.fn(),
    });
  });

  it('shows the authenticated account, avatar initial, and plan count', async () => {
    await render(<ProfileScreen />);
    expect(screen.getByText('Eduarda Souza')).toBeOnTheScreen();
    expect(screen.getByText('eduarda.souza@exemplo.com')).toBeOnTheScreen();
    expect(screen.getByText('E')).toBeOnTheScreen();
    expect(screen.getByText('3')).toBeOnTheScreen();
  });

  it('deletes the account through the API, then signs out', async () => {
    (api.deleteCurrentUser as jest.Mock).mockResolvedValue(undefined);
    const alertSpy = jest.spyOn(Alert, 'alert').mockImplementation((_title, _msg, buttons) => {
      const destructive = buttons?.find((b) => b.style === 'destructive');
      destructive?.onPress?.();
    });
    const user = userEvent.setup();
    await render(<ProfileScreen />);
    await user.press(screen.getByRole('link', { name: 'Excluir conta' }));
    await waitFor(() => expect(api.deleteCurrentUser).toHaveBeenCalled());
    await waitFor(() => expect(signOut).toHaveBeenCalledTimes(1));
    alertSpy.mockRestore();
  });
});
