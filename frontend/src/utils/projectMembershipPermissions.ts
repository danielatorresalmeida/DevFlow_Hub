import type {
  ManagedProjectMembershipRole,
  ProjectMembershipRole,
} from '../types/projectMembership'

const OWNER_ASSIGNABLE_ROLES: readonly ManagedProjectMembershipRole[] = [
  'MANAGER',
  'CONTRIBUTOR',
  'VIEWER',
]

const MANAGER_ASSIGNABLE_ROLES: readonly ManagedProjectMembershipRole[] = [
  'CONTRIBUTOR',
  'VIEWER',
]

export function canManageProjectMembers(
  actorRole: ProjectMembershipRole | null,
): boolean {
  return actorRole === 'OWNER' || actorRole === 'MANAGER'
}

export function getAssignableProjectMembershipRoles(
  actorRole: ProjectMembershipRole | null,
): readonly ManagedProjectMembershipRole[] {
  if (actorRole === 'OWNER') {
    return OWNER_ASSIGNABLE_ROLES
  }

  if (actorRole === 'MANAGER') {
    return MANAGER_ASSIGNABLE_ROLES
  }

  return []
}

export function canManageProjectMember(
  actorRole: ProjectMembershipRole | null,
  targetRole: ProjectMembershipRole,
): boolean {
  if (targetRole === 'OWNER') {
    return false
  }

  if (actorRole === 'OWNER') {
    return true
  }

  return (
    actorRole === 'MANAGER' &&
    (
      targetRole === 'CONTRIBUTOR' ||
      targetRole === 'VIEWER'
    )
  )
}
