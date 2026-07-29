export const MANAGED_PROJECT_MEMBERSHIP_ROLES = [
  'MANAGER',
  'CONTRIBUTOR',
  'VIEWER',
] as const

export type ManagedProjectMembershipRole =
  (typeof MANAGED_PROJECT_MEMBERSHIP_ROLES)[number]

export type ProjectMembershipRole =
  | 'OWNER'
  | ManagedProjectMembershipRole

export type ProjectMembershipStatus =
  | 'ACTIVE'
  | 'INACTIVE'

export interface ProjectMember {
  id: number
  projectId: number
  collaboratorId: number
  collaboratorName: string
  collaboratorRole: string
  role: ProjectMembershipRole
  status: ProjectMembershipStatus
  version: number
  createdAt: string
  updatedAt: string
}

export interface AddProjectMemberRequest {
  collaboratorId: number
  role: ManagedProjectMembershipRole
}

export interface UpdateProjectMemberRequest {
  role: ManagedProjectMembershipRole
  version: number
}
