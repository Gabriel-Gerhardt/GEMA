import { Text } from 'react-native';
import { act, render, screen, waitFor } from '@testing-library/react-native';
import { AuthProvider, useAuth } from './AuthContext';
import * as api from '../api/endpoints';
import { tokenStorage } from '../api/tokenStorage';

jest.mock('../api/endpoints');
jest.mock('../api/tokenStorage', () => ({
  tokenStorage: { get: jest.fn(), set: jest.fn(), clear: jest.fn() },
}));

const mockApi = api as jest.Mocked<typeof api>;
const mockStorage = tokenStorage as jest.Mocked<typeof tokenStorage>;

const ACCOUNT = { id: 1, username: 'alice@exemplo.com', name: 'Alice', role: 'USER', planCount: 2 };

let auth: ReturnType<typeof useAuth>;

function Probe() {
  auth = useAuth();
  return <Text>{auth.isRestoring ? 'restoring' : auth.isSignedIn ? 'in' : 'out'}</Text>;
}

async function renderProvider() {
  await render(
    <AuthProvider>
      <Probe />
    </AuthProvider>,
  );
  await waitFor(() => expect(screen.queryByText('restoring')).not.toBeOnTheScreen());
}

describe('AuthContext', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockStorage.get.mockResolvedValue(null);
    mockStorage.set.mockResolvedValue(undefined);
    mockStorage.clear.mockResolvedValue(undefined);
  });

  it('starts signed out when nothing is stored', async () => {
    await renderProvider();
    expect(screen.getByText('out')).toBeOnTheScreen();
    expect(mockApi.getCurrentUser).not.toHaveBeenCalled();
  });

  it('restores a session from a stored token', async () => {
    mockStorage.get.mockResolvedValue('stored-token');
    mockApi.getCurrentUser.mockResolvedValue(ACCOUNT);

    await renderProvider();

    expect(screen.getByText('in')).toBeOnTheScreen();
    expect(auth.user).toEqual(ACCOUNT);
  });

  it('discards a stored token the API no longer accepts', async () => {
    // It may have expired while the app was closed. Trusting it would leave the
    // app signed in and failing every call.
    mockStorage.get.mockResolvedValue('expired-token');
    mockApi.getCurrentUser.mockRejectedValue(new Error('401'));

    await renderProvider();

    expect(screen.getByText('out')).toBeOnTheScreen();
    expect(mockStorage.clear).toHaveBeenCalled();
  });

  it('signs in, stores the token and loads the account', async () => {
    mockApi.login.mockResolvedValue({ token: 'jwt', userId: 1, username: ACCOUNT.username, name: 'Alice' });
    mockApi.getCurrentUser.mockResolvedValue(ACCOUNT);
    await renderProvider();

    await act(async () => {
      await auth.signIn('alice@exemplo.com', 'senha12345');
    });

    expect(mockApi.login).toHaveBeenCalledWith('alice@exemplo.com', 'senha12345');
    expect(mockStorage.set).toHaveBeenCalledWith('jwt');
    expect(screen.getByText('in')).toBeOnTheScreen();
  });

  it('surfaces a failed sign-in instead of signing in', async () => {
    mockApi.login.mockRejectedValue(new Error('Sua sessão expirou. Entre novamente.'));
    await renderProvider();

    await expect(
      act(async () => {
        await auth.signIn('alice@exemplo.com', 'errada');
      }),
    ).rejects.toThrow();

    expect(screen.getByText('out')).toBeOnTheScreen();
    expect(mockStorage.set).not.toHaveBeenCalled();
  });

  it('registers and adopts the returned session', async () => {
    mockApi.register.mockResolvedValue({ token: 'jwt', userId: 1, username: ACCOUNT.username, name: 'Alice' });
    mockApi.getCurrentUser.mockResolvedValue(ACCOUNT);
    await renderProvider();

    await act(async () => {
      await auth.register('alice@exemplo.com', 'senha12345', 'Alice');
    });

    expect(mockApi.register).toHaveBeenCalledWith('alice@exemplo.com', 'senha12345', 'Alice');
    expect(screen.getByText('in')).toBeOnTheScreen();
  });

  it('clears the stored token on sign-out', async () => {
    mockStorage.get.mockResolvedValue('stored-token');
    mockApi.getCurrentUser.mockResolvedValue(ACCOUNT);
    await renderProvider();

    await act(async () => {
      await auth.signOut();
    });

    expect(mockStorage.clear).toHaveBeenCalled();
    expect(screen.getByText('out')).toBeOnTheScreen();
  });

  it('tears the session down when any call reports 401', async () => {
    // One place handles it, so the app never sits half-signed-in making calls
    // that keep failing.
    mockStorage.get.mockResolvedValue('stored-token');
    mockApi.getCurrentUser.mockResolvedValue(ACCOUNT);
    await renderProvider();

    await act(async () => {
      auth.onUnauthorized();
    });

    await waitFor(() => expect(screen.getByText('out')).toBeOnTheScreen());
    expect(mockStorage.clear).toHaveBeenCalled();
  });
});
