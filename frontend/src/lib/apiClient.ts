// Thin fetch wrapper for the GEMA backend. Normalizes the backend's
// ApiResponse error shape (see backend/.../config/ApiResponse.java and
// GlobalExceptionHandler.java) into a typed ApiError so pages don't need to
// know about the wire format.

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export interface ApiErrorBody {
  description: string
  message: string
  httpStatus: number
}

export class ApiError extends Error {
  readonly status: number
  readonly description: string

  constructor(body: ApiErrorBody) {
    super(body.message)
    this.status = body.httpStatus
    this.description = body.description
  }
}

/** Thrown for network failures, timeouts, or any non-JSON/unexpected error
 * response — i.e. anything that isn't a structured ApiError from the backend. */
export class NetworkError extends Error {}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      headers: { 'Content-Type': 'application/json', ...init?.headers },
      ...init,
    })
  } catch {
    throw new NetworkError('Network request failed')
  }

  if (response.status === 204) {
    return undefined as T
  }

  let data: unknown
  try {
    data = await response.json()
  } catch {
    if (response.ok) return undefined as T
    throw new NetworkError('Invalid response from server')
  }

  if (!response.ok) {
    const body = data as Partial<ApiErrorBody>
    throw new ApiError({
      description: body.description ?? 'ERROR',
      message: body.message ?? 'Something went wrong',
      httpStatus: body.httpStatus ?? response.status,
    })
  }

  return data as T
}

export const apiClient = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'POST', body: JSON.stringify(body) }),
}
