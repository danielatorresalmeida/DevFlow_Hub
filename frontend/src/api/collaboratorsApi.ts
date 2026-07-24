import { apiRequest } from './apiClient'
import type { AuthSession } from '../types/auth'
import type { Collaborator } from '../types/collaborator'

export function getCollaborators(
  session: AuthSession,
  signal?: AbortSignal,
): Promise<Collaborator[]> {
  return apiRequest<Collaborator[]>(
    '/api/collaborators',
    {
      method: 'GET',
      signal,
    },
    session,
  )
}