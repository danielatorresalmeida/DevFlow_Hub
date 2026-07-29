import {
  useState,
  type FormEvent,
} from 'react'
import { ApiClientError } from '../api/apiClient'
import {
  addProjectMember,
  removeProjectMember,
  updateProjectMemberRole,
} from '../api/projectMembershipsApi'
import type { AuthSession } from '../types/auth'
import type { Collaborator } from '../types/collaborator'
import type {
  ManagedProjectMembershipRole,
  ProjectMember,
  ProjectMembershipRole,
} from '../types/projectMembership'
import {
  canManageProjectMember,
  canManageProjectMembers,
  getAssignableProjectMembershipRoles,
} from '../utils/projectMembershipPermissions'

interface ProjectMembersPanelProps {
  projectId: number
  projectManagerId: number | null
  session: AuthSession
  collaborators: Collaborator[]
  members: ProjectMember[]
  onMembersChange: (members: ProjectMember[]) => void
}

interface ProjectMemberRowProps {
  projectId: number
  projectManagerId: number | null
  session: AuthSession
  actorRole: ProjectMembershipRole | null
  member: ProjectMember
  isBusy: boolean
  onBusyChange: (isBusy: boolean) => void
  onMemberUpdated: (member: ProjectMember) => void
  onMemberRemoved: (collaboratorId: number) => void
  onMessage: (message: ActionMessage) => void
}

type ActionMessage = {
  type: 'success' | 'error'
  text: string
}

const roleOrder: Record<ProjectMembershipRole, number> = {
  OWNER: 0,
  MANAGER: 1,
  CONTRIBUTOR: 2,
  VIEWER: 3,
}

function formatRole(role: string): string {
  return role
    .toLowerCase()
    .split('_')
    .map((part) => (
      part.charAt(0).toUpperCase() + part.slice(1)
    ))
    .join(' ')
}

function sortMembers(
  members: ProjectMember[],
): ProjectMember[] {
  return [...members].sort((left, right) => (
    roleOrder[left.role] - roleOrder[right.role] ||
    left.collaboratorName.localeCompare(
      right.collaboratorName,
    )
  ))
}

function getErrorMessage(
  error: unknown,
  fallback: string,
): string {
  if (error instanceof ApiClientError) {
    if (error.status === 409) {
      return `${error.message} Refresh the project and try again.`
    }

    return error.message
  }

  return error instanceof Error
    ? error.message
    : fallback
}

function ProjectMemberRow({
  projectId,
  projectManagerId,
  session,
  actorRole,
  member,
  isBusy,
  onBusyChange,
  onMemberUpdated,
  onMemberRemoved,
  onMessage,
}: ProjectMemberRowProps) {
  const assignableRoles =
    getAssignableProjectMembershipRoles(actorRole)

  const initialRole =
    member.role === 'OWNER'
      ? null
      : member.role

  const [selectedRole, setSelectedRole] =
    useState<ManagedProjectMembershipRole | null>(
      initialRole,
    )

  const canManageTarget = canManageProjectMember(
    actorRole,
    member.role,
  )

  const isCurrentProjectManager =
    projectManagerId === member.collaboratorId

  async function handleSaveRole() {
    if (
      !selectedRole ||
      selectedRole === member.role
    ) {
      return
    }

    onBusyChange(true)

    try {
      const updatedMember = await updateProjectMemberRole(
        projectId,
        member.collaboratorId,
        {
          role: selectedRole,
          version: member.version,
        },
        session,
      )

      onMemberUpdated(updatedMember)
      setSelectedRole(updatedMember.role === 'OWNER'
        ? null
        : updatedMember.role)

      onMessage({
        type: 'success',
        text: `${updatedMember.collaboratorName}'s role was updated.`,
      })
    } catch (error) {
      onMessage({
        type: 'error',
        text: getErrorMessage(
          error,
          'The membership role could not be updated.',
        ),
      })
    } finally {
      onBusyChange(false)
    }
  }

  async function handleRemove() {
    const confirmed = window.confirm(
      `Remove ${member.collaboratorName} from this project?`,
    )

    if (!confirmed) {
      return
    }

    onBusyChange(true)

    try {
      await removeProjectMember(
        projectId,
        member.collaboratorId,
        session,
      )

      onMemberRemoved(member.collaboratorId)
      onMessage({
        type: 'success',
        text: `${member.collaboratorName} was removed from the project.`,
      })
    } catch (error) {
      onMessage({
        type: 'error',
        text: getErrorMessage(
          error,
          'The project member could not be removed.',
        ),
      })
    } finally {
      onBusyChange(false)
    }
  }

  return (
    <article className="project-member-item">
      <header className="project-member-header">
        <div>
          <h3>{member.collaboratorName}</h3>
          <p>{member.collaboratorRole}</p>
        </div>

        <span
          className={`membership-role-badge membership-role-badge--${member.role.toLowerCase()}`}
        >
          {formatRole(member.role)}
        </span>
      </header>

      {canManageTarget &&
        !isCurrentProjectManager &&
        selectedRole && (
          <div className="project-member-actions">
            <label className="project-member-field">
              <span>
                Role for {member.collaboratorName}
              </span>
              <select
                value={selectedRole}
                onChange={(event) => {
                  setSelectedRole(
                    event.target
                      .value as ManagedProjectMembershipRole,
                  )
                }}
                disabled={isBusy}
              >
                {assignableRoles.map((role) => (
                  <option key={role} value={role}>
                    {formatRole(role)}
                  </option>
                ))}
              </select>
            </label>

            <div className="project-member-action-buttons">
              <button
                className="task-action-button task-action-button--secondary"
                type="button"
                onClick={() => {
                  void handleSaveRole()
                }}
                disabled={
                  isBusy ||
                  selectedRole === member.role
                }
              >
                Save role
              </button>

              <button
                className="task-action-button task-action-button--danger"
                type="button"
                onClick={() => {
                  void handleRemove()
                }}
                disabled={isBusy}
              >
                Remove member
              </button>
            </div>
          </div>
        )}

      {member.role === 'OWNER' && (
        <p className="project-member-note">
          Project ownership is managed through the dedicated
          ownership transfer operation.
        </p>
      )}

      {member.role !== 'OWNER' &&
        isCurrentProjectManager && (
          <p className="project-member-note">
            Select another project manager before removing or
            demoting this member.
          </p>
        )}

      {member.role !== 'OWNER' &&
        !isCurrentProjectManager &&
        !canManageTarget && (
          <p className="project-member-note">
            Your role cannot manage this membership.
          </p>
        )}
    </article>
  )
}

export function ProjectMembersPanel({
  projectId,
  projectManagerId,
  session,
  collaborators,
  members,
  onMembersChange,
}: ProjectMembersPanelProps) {
  const actorRole =
    members.find((member) => (
      member.collaboratorId ===
      session.collaborator.id
    ))?.role ?? null

  const assignableRoles =
    getAssignableProjectMembershipRoles(actorRole)

  const availableCollaborators = collaborators
    .filter((collaborator) => (
      collaborator.active &&
      !members.some((member) => (
        member.collaboratorId === collaborator.id
      ))
    ))
    .sort((left, right) => (
      left.name.localeCompare(right.name)
    ))

  const [selectedCollaboratorId, setSelectedCollaboratorId] =
    useState('')

  const [selectedRole, setSelectedRole] =
    useState<ManagedProjectMembershipRole>(
      assignableRoles[0] ?? 'CONTRIBUTOR',
    )

  const [isBusy, setIsBusy] = useState(false)
  const [message, setMessage] =
    useState<ActionMessage | null>(null)

  const canManage =
    canManageProjectMembers(actorRole)

  function replaceMember(updatedMember: ProjectMember) {
    onMembersChange(
      sortMembers(
        members.map((member) => (
          member.collaboratorId ===
          updatedMember.collaboratorId
            ? updatedMember
            : member
        )),
      ),
    )
  }

  async function handleAddMember(
    event: FormEvent<HTMLFormElement>,
  ) {
    event.preventDefault()

    const collaboratorId = Number(
      selectedCollaboratorId,
    )

    if (
      !Number.isSafeInteger(collaboratorId) ||
      collaboratorId <= 0 ||
      !assignableRoles.includes(selectedRole)
    ) {
      setMessage({
        type: 'error',
        text: 'Select a collaborator and an allowed membership role.',
      })
      return
    }

    setIsBusy(true)
    setMessage(null)

    try {
      const addedMember = await addProjectMember(
        projectId,
        {
          collaboratorId,
          role: selectedRole,
        },
        session,
      )

      onMembersChange(
        sortMembers([...members, addedMember]),
      )

      setSelectedCollaboratorId('')
      setMessage({
        type: 'success',
        text: `${addedMember.collaboratorName} was added to the project.`,
      })
    } catch (error) {
      setMessage({
        type: 'error',
        text: getErrorMessage(
          error,
          'The project member could not be added.',
        ),
      })
    } finally {
      setIsBusy(false)
    }
  }

  return (
    <section className="project-members-panel">
      <div className="project-detail-section-heading">
        <div>
          <p className="eyebrow">Project access</p>
          <h2>Members</h2>
        </div>

        <span className="project-member-count">
          {members.length}{' '}
          {members.length === 1 ? 'member' : 'members'}
        </span>
      </div>

      {canManage && (
        <form
          className="project-member-form"
          onSubmit={handleAddMember}
        >
          <div className="project-member-form-heading">
            <div>
              <h3>Add member</h3>
              <p>
                Add an active collaborator with an allowed
                project role.
              </p>
            </div>

            <span className="membership-role-badge">
              Your role: {formatRole(actorRole ?? '')}
            </span>
          </div>

          <div className="project-member-form-grid">
            <label className="project-member-field">
              <span>Collaborator</span>
              <select
                value={selectedCollaboratorId}
                onChange={(event) => {
                  setSelectedCollaboratorId(
                    event.target.value,
                  )
                }}
                disabled={
                  isBusy ||
                  availableCollaborators.length === 0
                }
              >
                <option value="">
                  {availableCollaborators.length === 0
                    ? 'No collaborators available'
                    : 'Select a collaborator'}
                </option>

                {availableCollaborators.map(
                  (collaborator) => (
                    <option
                      key={collaborator.id}
                      value={collaborator.id}
                    >
                      {collaborator.name} ({collaborator.email})
                    </option>
                  ),
                )}
              </select>
            </label>

            <label className="project-member-field">
              <span>Membership role</span>
              <select
                value={selectedRole}
                onChange={(event) => {
                  setSelectedRole(
                    event.target
                      .value as ManagedProjectMembershipRole,
                  )
                }}
                disabled={isBusy}
              >
                {assignableRoles.map((role) => (
                  <option key={role} value={role}>
                    {formatRole(role)}
                  </option>
                ))}
              </select>
            </label>

            <button
              className="task-action-button task-action-button--primary"
              type="submit"
              disabled={
                isBusy ||
                selectedCollaboratorId === '' ||
                availableCollaborators.length === 0
              }
            >
              {isBusy ? 'Saving...' : 'Add member'}
            </button>
          </div>
        </form>
      )}

      {!canManage && (
        <section className="project-members-readonly">
          <strong>Read-only access</strong>
          <p>
            Your project membership allows you to view the
            member list, but not to change it.
          </p>
        </section>
      )}

      {message && (
        <p
          className={
            message.type === 'success'
              ? 'project-member-message project-member-message--success'
              : 'project-member-message project-member-message--error'
          }
          role={message.type === 'error' ? 'alert' : 'status'}
        >
          {message.text}
        </p>
      )}

      <div className="project-member-list">
        {sortMembers(members).map((member) => (
          <ProjectMemberRow
            key={member.id}
            projectId={projectId}
            projectManagerId={projectManagerId}
            session={session}
            actorRole={actorRole}
            member={member}
            isBusy={isBusy}
            onBusyChange={setIsBusy}
            onMemberUpdated={replaceMember}
            onMemberRemoved={(collaboratorId) => {
              onMembersChange(
                members.filter((currentMember) => (
                  currentMember.collaboratorId !==
                  collaboratorId
                )),
              )
            }}
            onMessage={setMessage}
          />
        ))}
      </div>
    </section>
  )
}
