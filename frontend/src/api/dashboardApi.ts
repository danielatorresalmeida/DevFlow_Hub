import { apiRequest } from './apiClient'
import type { AuthSession } from '../types/auth'
import type { DashboardSummary } from '../types/dashboard'

export function getDashboardSummary(
  session: AuthSession,
  signal?: AbortSignal,
): Promise<DashboardSummary> {
  return apiRequest<DashboardSummary>(
    '/api/dashboard',
    {
      method: 'GET',
      signal,
    },
    session,
  )
}