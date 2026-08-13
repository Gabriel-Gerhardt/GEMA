/** Wire shapes returned by the API. Kept separate from the app's own `Plan`
 * model in `state/types.ts`, which is what the screens render. */

export interface AuthResponse {
  token: string;
  userId: number;
  username: string;
  name: string | null;
}

export interface UserDetailsResponse {
  id: number;
  username: string;
  name: string | null;
  role: string;
  planCount: number;
}

export interface QrcodeResponse {
  publicId: string;
  title: string;
  content: string | null;
  ownerName: string | null;
  emergencyContactName: string | null;
  emergencyContactPhone: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UserQrcodeResponse {
  publicId: string;
  title: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SectionResponse {
  id: number;
  qrcodePublicId: string;
  title: string;
  content: string;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
}

/** Spring Data's page envelope, narrowed to what the app reads. */
export interface Page<T> {
  content: T[];
  totalElements: number;
  number: number;
  size: number;
  last: boolean;
}

export interface SectionPayload {
  title: string;
  content: string;
}
