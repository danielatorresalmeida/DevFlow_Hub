import { apiRequest } from './apiClient'
import type { AuthSession } from '../types/auth'
import type {
  Project,
  ProjectInput,
} from '../types/project'

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

export function createProject(
  input: ProjectInput,
  session: AuthSession,
): Promise<Project> {
  return apiRequest<Project>(
    '/api/projects',
    {
      method: 'POST',
      body: JSON.stringify(input),
    },
    session,
  )
}

export function updateProject(
  projectId: number,
  input: ProjectInput,
  session: AuthSession,
): Promise<Project> {
  return apiRequest<Project>(
    `/api/projects/${projectId}`,
    {
      method: 'PUT',
      body: JSON.stringify(input),
    },
    session,
  )
}

export function deleteProject(
  projectId: number,
  session: AuthSession,
): Promise<void> {
  return apiRequest<void>(
    `/api/projects/${projectId}`,
    {
      method: 'DELETE',
    },
    session,
  )
}
