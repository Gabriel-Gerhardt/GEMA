import { API_BASE_URL } from './config';
import { tokenStorage } from './tokenStorage';

/**
 * An API call that came back with a non-2xx status.
 *
 * Carries the HTTP status so callers can distinguish the cases that mean
 * something to a user — 401 (the session is gone), 409 (that email is taken) —
 * from the ones that only mean "it failed".
 */
export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

/** The API is unreachable: the server is down, or the device is offline. */
export class NetworkError extends Error {
  constructor() {
    super('Não foi possível falar com o servidor.');
    this.name = 'NetworkError';
  }
}

/** The error envelope the backend's GlobalExceptionHandler returns. */
interface ApiErrorBody {
  description?: string;
  message?: string;
  httpStatus?: number;
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  body?: unknown;
  /** Public routes (`/api/q/**`) must not send a token; the finder has no account. */
  authenticated?: boolean;
  /** Invoked on a 401 so the session can be torn down centrally. */
  onUnauthorized?: () => void;
}

const FRIENDLY_MESSAGES: Record<number, string> = {
  401: 'Sua sessão expirou. Entre novamente.',
  403: 'Você não tem acesso a isso.',
  404: 'Não encontramos o que você procurava.',
  409: 'Esse email já está em uso.',
};

async function readError(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as ApiErrorBody;
    // Prefer the server's own message for 400s — it names the offending field,
    // which is exactly what a user needs to fix a form.
    if (response.status === 400 && body.message) {
      return body.message;
    }
  } catch {
    // Not JSON, or an empty body. Fall through to the generic message.
  }
  return FRIENDLY_MESSAGES[response.status] ?? 'Algo deu errado. Tente de novo.';
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, authenticated = true, onUnauthorized } = options;

  const headers: Record<string, string> = {};
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  if (authenticated) {
    const token = await tokenStorage.get();
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
  }

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    // fetch only rejects when the request never completed, so this is always a
    // transport problem rather than an error the API chose to return.
    throw new NetworkError();
  }

  if (response.status === 401 && authenticated) {
    onUnauthorized?.();
  }

  if (!response.ok) {
    throw new ApiError(response.status, await readError(response));
  }

  // 204 and 202 carry no body; asking for JSON would throw.
  if (response.status === 204 || response.status === 202) {
    return undefined as T;
  }
  return (await response.json()) as T;
}
