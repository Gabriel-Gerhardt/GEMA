import { render, screen, userEvent } from '@testing-library/react-native';
import { Alert } from 'react-native';
import { ProfileScreen } from './ProfileScreen';
import { usePlans } from '../state/PlansContext';
import { useAuth } from '../state/AuthContext';

jest.mock('../state/PlansContext');
jest.mock('../state/AuthContext');

describe('ProfileScreen', () => {
  const signOut = jest.fn();
  beforeEach(() => {
    signOut.mockClear();
    (usePlans as jest.Mock).mockReturnValue({ plans: [{}, {}, {}] });
    (useAuth as jest.Mock).mockReturnValue({ signOut });
  });

  it('shows the mock user, avatar initial, and plan count', async () => {
    await render(<ProfileScreen />);
    expect(screen.getByText('Eduarda Souza')).toBeOnTheScreen();
    expect(screen.getByText('eduarda.souza@exemplo.com')).toBeOnTheScreen();
    expect(screen.getByText('E')).toBeOnTheScreen();
    expect(screen.getByText('3')).toBeOnTheScreen();
  });

  it('signs out once the delete-account confirm dialog is accepted', async () => {
    const alertSpy = jest.spyOn(Alert, 'alert').mockImplementation((_title, _msg, buttons) => {
      const destructive = buttons?.find((b) => b.style === 'destructive');
      destructive?.onPress?.();
    });
    const user = userEvent.setup();
    await render(<ProfileScreen />);
    await user.press(screen.getByRole('link', { name: 'Excluir conta' }));
    expect(signOut).toHaveBeenCalledTimes(1);
    alertSpy.mockRestore();
  });
});
