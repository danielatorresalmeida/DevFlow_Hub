import { apiRequest } from './apiClient'
import type { LoginRequest, LoginResponse } from '../types/auth'

export function authenticate(
  credentials: LoginRequest,
): Promise<LoginResponse> {
  return apiRequest<LoginResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(credentials),
  })
}
