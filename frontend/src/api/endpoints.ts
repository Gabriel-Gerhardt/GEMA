import { request } from './client';
import type {
  AuthResponse,
  Page,
  QrcodeResponse,
  SectionPayload,
  SectionResponse,
  UserDetailsResponse,
  UserQrcodeResponse,
} from './types';

type Unauthorized = { onUnauthorized?: () => void };

// ---------------------------------------------------------------------------
// auth — none of these carry a token

export function register(username: string, password: string, name?: string) {
  return request<AuthResponse>('/api/users', {
    method: 'POST',
    authenticated: false,
    body: { username, password, name },
  });
}

export function login(username: string, password: string) {
  return request<AuthResponse>('/api/auth/login', {
    method: 'POST',
    authenticated: false,
    body: { username, password },
  });
}

/** Answers 202 whether or not the account exists, by design. */
export function requestPasswordReset(username: string) {
  return request<void>('/api/auth/password-reset', {
    method: 'POST',
    authenticated: false,
    body: { username },
  });
}

export function confirmPasswordReset(token: string, newPassword: string) {
  return request<void>('/api/auth/password-reset/confirm', {
    method: 'POST',
    authenticated: false,
    body: { token, newPassword },
  });
}

// ---------------------------------------------------------------------------
// account

export function getCurrentUser(opts: Unauthorized = {}) {
  return request<UserDetailsResponse>('/api/users/me', opts);
}

export function updateCurrentUser(name: string, opts: Unauthorized = {}) {
  return request<UserDetailsResponse>('/api/users/me', { ...opts, method: 'PUT', body: { name } });
}

export function deleteCurrentUser(opts: Unauthorized = {}) {
  return request<void>('/api/users/me', { ...opts, method: 'DELETE' });
}

// ---------------------------------------------------------------------------
// the caller's own plans

export function listPlans(page = 0, size = 50, opts: Unauthorized = {}) {
  return request<Page<UserQrcodeResponse>>(`/api/qrcodes?page=${page}&size=${size}`, opts);
}

export function getPlan(publicId: string, opts: Unauthorized = {}) {
  return request<QrcodeResponse>(`/api/qrcodes/${publicId}`, opts);
}

export function getPlanSections(publicId: string, opts: Unauthorized = {}) {
  return request<SectionResponse[]>(`/api/qrcodes/${publicId}/sections`, opts);
}

/** Plan and sections in one call, so a failure cannot leave an empty plan behind. */
export function createPlan(
  body: {
    title: string;
    ownerName?: string;
    emergencyContactName?: string;
    emergencyContactPhone?: string;
    sections?: SectionPayload[];
  },
  opts: Unauthorized = {},
) {
  return request<QrcodeResponse>('/api/qrcodes', { ...opts, method: 'POST', body });
}

export function updatePlan(
  publicId: string,
  body: {
    title: string;
    isActive: boolean;
    ownerName?: string | null;
    emergencyContactName?: string | null;
    emergencyContactPhone?: string | null;
  },
  opts: Unauthorized = {},
) {
  return request<QrcodeResponse>(`/api/qrcodes/${publicId}`, { ...opts, method: 'PUT', body });
}

export function replaceSections(publicId: string, sections: SectionPayload[], opts: Unauthorized = {}) {
  return request<SectionResponse[]>(`/api/qrcodes/${publicId}/sections`, {
    ...opts,
    method: 'PUT',
    body: { sections },
  });
}

export function deletePlan(publicId: string, opts: Unauthorized = {}) {
  return request<void>(`/api/qrcodes/${publicId}`, { ...opts, method: 'DELETE' });
}

/** The owner's QR image. Authenticated, unlike the guide it points at. */
export function planImageUrl(publicId: string) {
  return `/api/qrcodes/${publicId}/image`;
}

// ---------------------------------------------------------------------------
// the public guide — deliberately unauthenticated

export function getPublicPlan(publicId: string) {
  return request<QrcodeResponse>(`/api/q/${publicId}`, { authenticated: false });
}

export function getPublicSections(publicId: string) {
  return request<SectionResponse[]>(`/api/q/${publicId}/sections`, { authenticated: false });
}
