import {
  useState,
  type FormEvent,
} from 'react'
import { ApiClientError } from '../api/apiClient'
import { transferProjectOwnership } from '../api/projectOwnershipApi'
import type { AuthSession } from '../types/auth'
import type {
  ProjectMember,
  ProjectOwnershipTransferResponse,
} from '../types/projectMembership'

interface ProjectOwnershipTransferPanelProps {
  projectId: number
  session: AuthSession
  members: ProjectMember[]
  onTransferComplete: (
    response: ProjectOwnershipTransferResponse,
  ) => void
  onRefresh: () => void
}

type ActionMessage = {
  type: 'error'
  text: string
  isConflict: boolean
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

function getErrorMessage(
  error: unknown,
): ActionMessage {
  if (error instanceof ApiClientError) {
    return {
      type: 'error',
      text: error.message,
      isConflict: error.status === 409,
    }
  }

  return {
    type: 'error',
    text: error instanceof Error
      ? error.message
      : 'Project ownership could not be transferred.',
    isConflict: false,
  }
}

export function ProjectOwnershipTransferPanel({
  projectId,
  session,
  members,
  onTransferComplete,
  onRefresh,
}: ProjectOwnershipTransferPanelProps) {
  const actorMembership = members.find((member) => (
    member.collaboratorId ===
      session.collaborator.id &&
    member.status === 'ACTIVE'
  ))

  const currentOwner = members.find((member) => (
    member.role === 'OWNER' &&
    member.status === 'ACTIVE'
  ))

  const eligibleMembers = members
    .filter((member) => (
      member.status === 'ACTIVE' &&
      member.role !== 'OWNER' &&
      member.collaboratorId !==
        session.collaborator.id
    ))
    .sort((left, right) => (
      left.collaboratorName.localeCompare(
        right.collaboratorName,
      )
    ))

  const [selectedCollaboratorId, setSelectedCollaboratorId] =
    useState('')

  const [hasConfirmedImpact, setHasConfirmedImpact] =
    useState(false)

  const [isBusy, setIsBusy] = useState(false)
  const [message, setMessage] =
    useState<ActionMessage | null>(null)

  if (
    actorMembership?.role !== 'OWNER' ||
    !currentOwner
  ) {
    return null
  }

  const ownerMembership = currentOwner

  const selectedMember = eligibleMembers.find(
    (member) => (
      member.collaboratorId ===
      Number(selectedCollaboratorId)
    ),
  )

  async function handleSubmit(
    event: FormEvent<HTMLFormElement>,
  ) {
    event.preventDefault()
    setMessage(null)

    if (!selectedMember || !hasConfirmedImpact) {
      setMessage({
        type: 'error',
        text: 'Select an active project member and confirm the ownership impact.',
        isConflict: false,
      })
      return
    }

    const confirmed = window.confirm(
      `Transfer ownership to ${selectedMember.collaboratorName}? ` +
      'You will become a Manager and the selected member will become the project Owner and manager.',
    )

    if (!confirmed) {
      return
    }

    setIsBusy(true)

    try {
      const response = await transferProjectOwnership(
        projectId,
        {
          newOwnerCollaboratorId:
            selectedMember.collaboratorId,
          currentOwnerMembershipVersion:
            ownerMembership.version,
          newOwnerMembershipVersion:
            selectedMember.version,
        },
        session,
      )

      onTransferComplete(response)
      setSelectedCollaboratorId('')
      setHasConfirmedImpact(false)
    } catch (error) {
      setMessage(getErrorMessage(error))
    } finally {
      setIsBusy(false)
    }
  }

  return (
    <section className="project-ownership-transfer-panel">
      <div className="project-detail-section-heading">
        <div>
          <p className="eyebrow">Project ownership</p>
          <h2>Transfer ownership</h2>
        </div>

        <span className="membership-role-badge membership-role-badge--owner">
          Owner only
        </span>
      </div>

      <div className="project-ownership-warning">
        <strong>This is a high-impact operation.</strong>
        <p>
          The selected member will become both project Owner
          and manager. Your membership will change from Owner
          to Manager immediately.
        </p>
      </div>

      <form
        className="project-ownership-transfer-form"
        onSubmit={handleSubmit}
      >
        <label className="project-member-field">
          <span>New owner</span>
          <select
            value={selectedCollaboratorId}
            onChange={(event) => {
              setSelectedCollaboratorId(
                event.target.value,
              )
              setHasConfirmedImpact(false)
              setMessage(null)
            }}
            disabled={
              isBusy ||
              eligibleMembers.length === 0
            }
          >
            <option value="">
              {eligibleMembers.length === 0
                ? 'No eligible members available'
                : 'Select a project member'}
            </option>

            {eligibleMembers.map((member) => (
              <option
                key={member.id}
                value={member.collaboratorId}
              >
                {`${member.collaboratorName} (${formatRole(member.role)})`}
              </option>
            ))}
          </select>
        </label>

        <label className="project-ownership-confirmation">
          <input
            type="checkbox"
            checked={hasConfirmedImpact}
            onChange={(event) => {
              setHasConfirmedImpact(
                event.target.checked,
              )
              setMessage(null)
            }}
            disabled={
              isBusy ||
              !selectedMember
            }
          />

          <span>
            I understand that ownership and project management
            responsibility will move to the selected member.
          </span>
        </label>

        <button
          className="task-action-button task-action-button--danger"
          type="submit"
          disabled={
            isBusy ||
            !selectedMember ||
            !hasConfirmedImpact
          }
        >
          {isBusy
            ? 'Transferring...'
            : 'Transfer ownership'}
        </button>
      </form>

      {message && (
        <div
          className="project-member-message project-member-message--error project-ownership-error"
          role="alert"
        >
          <p>{message.text}</p>

          {message.isConflict && (
            <button
              className="secondary-button"
              type="button"
              onClick={onRefresh}
              disabled={isBusy}
            >
              Refresh project data
            </button>
          )}
        </div>
      )}
    </section>
  )
}
