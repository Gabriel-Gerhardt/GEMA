import { ApiError, NetworkError, request } from './client';
import { tokenStorage } from './tokenStorage';

jest.mock('./tokenStorage', () => ({
  tokenStorage: { get: jest.fn(), set: jest.fn(), clear: jest.fn() },
}));

const mockStorage = tokenStorage as jest.Mocked<typeof tokenStorage>;

function respond(status: number, body?: unknown) {
  return Promise.resolve({
    ok: status >= 200 && status < 300,
    status,
    json: async () => {
      if (body === undefined) throw new Error('no body');
      return body;
    },
  } as Response);
}

describe('api client', () => {
  const fetchMock = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    globalThis.fetch = fetchMock as unknown as typeof fetch;
    mockStorage.get.mockResolvedValue(null);
  });

  it('attaches the stored token as a bearer header', async () => {
    mockStorage.get.mockResolvedValue('jwt-token');
    fetchMock.mockReturnValue(respond(200, { ok: true }));

    await request('/api/users/me');

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/users/me',
      expect.objectContaining({ headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }) }),
    );
  });

  it('sends no token on a public route', async () => {
    // The finder reading a scanned guide has no account; a token there would be
    // meaningless, and reading storage for one is pointless work.
    mockStorage.get.mockResolvedValue('jwt-token');
    fetchMock.mockReturnValue(respond(200, {}));

    await request('/api/q/abc123', { authenticated: false });

    const [, init] = fetchMock.mock.calls[0];
    expect(init.headers.Authorization).toBeUndefined();
    expect(mockStorage.get).not.toHaveBeenCalled();
  });

  it('reports a 401 so the session can be torn down in one place', async () => {
    const onUnauthorized = jest.fn();
    fetchMock.mockReturnValue(respond(401));

    await expect(request('/api/users/me', { onUnauthorized })).rejects.toBeInstanceOf(ApiError);

    expect(onUnauthorized).toHaveBeenCalled();
  });

  it('does not report a 401 from a public route as a lost session', async () => {
    // A stale token in a shared browser must not sign the user out because a
    // public guide happened to answer 401.
    const onUnauthorized = jest.fn();
    fetchMock.mockReturnValue(respond(401));

    await expect(request('/api/q/abc', { authenticated: false, onUnauthorized })).rejects.toBeInstanceOf(ApiError);

    expect(onUnauthorized).not.toHaveBeenCalled();
  });

  it('carries the status so callers can tell 404 from a failure', async () => {
    fetchMock.mockReturnValue(respond(404));

    await expect(request('/api/q/nope', { authenticated: false })).rejects.toMatchObject({ status: 404 });
  });

  it('surfaces the API’s own message for a validation error', async () => {
    // A 400 names the offending field, which is exactly what a person needs to
    // fix the form; a generic message would throw that away.
    fetchMock.mockReturnValue(
      respond(400, { description: 'VALIDATION_ERROR', message: 'password: size must be between 8 and 72', httpStatus: 400 }),
    );

    await expect(request('/api/users', { method: 'POST', authenticated: false, body: {} })).rejects.toThrow(
      'password: size must be between 8 and 72',
    );
  });

  it('uses a readable message for a conflict rather than the raw envelope', async () => {
    fetchMock.mockReturnValue(respond(409, { message: 'Username already exists' }));

    await expect(request('/api/users', { method: 'POST', authenticated: false, body: {} })).rejects.toThrow(
      'Esse email já está em uso.',
    );
  });

  it('treats an unreachable server as a network error, not an API error', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'));

    await expect(request('/api/users/me')).rejects.toBeInstanceOf(NetworkError);
  });

  it('does not try to parse a body from 204 or 202', async () => {
    // DELETE answers 204 and the reset request answers 202; asking for JSON
    // would throw on an empty body.
    fetchMock.mockReturnValue(respond(204));
    await expect(request('/api/users/me', { method: 'DELETE' })).resolves.toBeUndefined();

    fetchMock.mockReturnValue(respond(202));
    await expect(
      request('/api/auth/password-reset', { method: 'POST', authenticated: false, body: {} }),
    ).resolves.toBeUndefined();
  });

  it('falls back to a generic message when the error body is not JSON', async () => {
    fetchMock.mockReturnValue(respond(500));

    await expect(request('/api/users/me')).rejects.toThrow('Algo deu errado. Tente de novo.');
  });
});
