import { apiRequest } from './apiClient'
import type { AuthSession } from '../types/auth'
import type { Project } from '../types/project'

export function getProjects(
  session: AuthSession,
  signal?: AbortSignal,
): Promise<Project[]> {
  return apiRequest<Project[]>(
    '/api/projects',
    {
      method: 'GET',
      signal,
    },
    session,
  )
}

export function getProjectById(
  projectId: number,
  session: AuthSession,
  signal?: AbortSignal,
): Promise<Project> {
  return apiRequest<Project>(
    `/api/projects/${projectId}`,
    {
      method: 'GET',
      signal,
    },
    session,
  )
}