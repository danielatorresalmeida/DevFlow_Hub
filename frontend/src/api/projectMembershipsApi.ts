import { apiRequest } from './apiClient'
import type { AuthSession } from '../types/auth'
import type {
  AddProjectMemberRequest,
  ProjectMember,
  UpdateProjectMemberRequest,
} from '../types/projectMembership'

export function getProjectMembers(
  projectId: number,
  session: AuthSession,
  signal?: AbortSignal,
): Promise<ProjectMember[]> {
  return apiRequest<ProjectMember[]>(
    `/api/projects/${projectId}/members`,
    {
      method: 'GET',
      signal,
    },
    session,
  )
}

export function addProjectMember(
  projectId: number,
  request: AddProjectMemberRequest,
  session: AuthSession,
): Promise<ProjectMember> {
  return apiRequest<ProjectMember>(
    `/api/projects/${projectId}/members`,
    {
      method: 'POST',
      body: JSON.stringify(request),
    },
    session,
  )
}

export function updateProjectMemberRole(
  projectId: number,
  collaboratorId: number,
  request: UpdateProjectMemberRequest,
  session: AuthSession,
): Promise<ProjectMember> {
  return apiRequest<ProjectMember>(
    `/api/projects/${projectId}/members/${collaboratorId}`,
    {
      method: 'PATCH',
      body: JSON.stringify(request),
    },
    session,
  )
}

export function removeProjectMember(
  projectId: number,
  collaboratorId: number,
  session: AuthSession,
): Promise<void> {
  return apiRequest<void>(
    `/api/projects/${projectId}/members/${collaboratorId}`,
    {
      method: 'DELETE',
    },
    session,
  )
}
