import { useState } from 'react'
import { ApiClientError } from '../api/apiClient'
import {
  createProject,
  updateProject,
} from '../api/projectsApi'
import type { AuthSession } from '../types/auth'
import type {
  Project,
  ProjectInput,
  ProjectStatus,
} from '../types/project'

type ProjectFormMode = 'create' | 'edit'

export interface ProjectManagerOption {
  id: number
  name: string
}

interface ProjectFormProps {
  mode: ProjectFormMode
  session: AuthSession
  initialProject?: Project
  managerOptions?: ProjectManagerOption[]
  onSaved: (project: Project) => void
  onCancel: () => void
}

interface ProjectFormValues {
  name: string
  description: string
  status: ProjectStatus
  startDate: string
  endDate: string
  managerId: string
}

const projectStatuses: ProjectStatus[] = [
  'PLANNED',
  'IN_PROGRESS',
  'COMPLETED',
]

function formatLabel(value: string): string {
  return value
    .toLowerCase()
    .split('_')
    .map((part) => (
      part.charAt(0).toUpperCase() + part.slice(1)
    ))
    .join(' ')
}

function createInitialValues(
  session: AuthSession,
  initialProject?: Project,
): ProjectFormValues {
  return {
    name: initialProject?.name ?? '',
    description: initialProject?.description ?? '',
    status: (initialProject?.status as ProjectStatus | undefined) ?? 'PLANNED',
    startDate: initialProject?.startDate ?? '',
    endDate: initialProject?.endDate ?? '',
    managerId: String(
      initialProject?.managerId ?? session.collaborator.id,
    ),
  }
}

function getErrorMessage(error: unknown): string {
  if (error instanceof ApiClientError) {
    const validationMessage = Object.values(
      error.validationErrors,
    )[0]

    return validationMessage ?? error.message
  }

  return error instanceof Error
    ? error.message
    : 'The project could not be saved.'
}

export function ProjectForm({
  mode,
  session,
  initialProject,
  managerOptions = [],
  onSaved,
  onCancel,
}: ProjectFormProps) {
  const [values, setValues] = useState(
    () => createInitialValues(session, initialProject),
  )
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] =
    useState<string | null>(null)

  const isEdit = mode === 'edit'

  function updateValue<K extends keyof ProjectFormValues>(
    field: K,
    value: ProjectFormValues[K],
  ) {
    setValues((current) => ({
      ...current,
      [field]: value,
    }))
    setErrorMessage(null)
  }

  async function handleSubmit(
    event: React.FormEvent<HTMLFormElement>,
  ) {
    event.preventDefault()

    const name = values.name.trim()
    const description = values.description.trim()

    if (!name) {
      setErrorMessage('Project name is required.')
      return
    }

    if (name.length > 150) {
      setErrorMessage(
        'Project name must have at most 150 characters.',
      )
      return
    }

    if (description.length > 3000) {
      setErrorMessage(
        'Project description must have at most 3000 characters.',
      )
      return
    }

    if (
      values.startDate &&
      values.endDate &&
      values.endDate < values.startDate
    ) {
      setErrorMessage(
        'Project end date cannot be before its start date.',
      )
      return
    }

    const managerId = isEdit
      ? Number(values.managerId)
      : session.collaborator.id

    if (!Number.isSafeInteger(managerId) || managerId <= 0) {
      setErrorMessage('Select a valid project manager.')
      return
    }

    const input: ProjectInput = {
      name,
      description: description || null,
      status: values.status,
      startDate: values.startDate || null,
      endDate: values.endDate || null,
      managerId,
    }

    setIsSubmitting(true)
    setErrorMessage(null)

    try {
      const savedProject = isEdit && initialProject
        ? await updateProject(
            initialProject.id,
            input,
            session,
          )
        : await createProject(input, session)

      onSaved(savedProject)
    } catch (error) {
      setErrorMessage(getErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form
      className="task-form"
      onSubmit={(event) => {
        void handleSubmit(event)
      }}
    >
      <div className="task-form-heading">
        <div>
          <p className="eyebrow">
            {isEdit ? 'Project settings' : 'New project'}
          </p>
          <h2>
            {isEdit ? 'Edit project' : 'Create project'}
          </h2>
        </div>

        <button
          className="secondary-button"
          type="button"
          onClick={onCancel}
          disabled={isSubmitting}
        >
          Cancel
        </button>
      </div>

      <div className="task-form-grid">
        <label className="task-form-field task-form-field--wide">
          <span>Name</span>
          <input
            value={values.name}
            onChange={(event) => {
              updateValue('name', event.target.value)
            }}
            maxLength={150}
            disabled={isSubmitting}
          />
        </label>

        <label className="task-form-field task-form-field--wide">
          <span>Description</span>
          <textarea
            value={values.description}
            onChange={(event) => {
              updateValue('description', event.target.value)
            }}
            maxLength={3000}
            rows={5}
            disabled={isSubmitting}
          />
        </label>

        <label className="task-form-field">
          <span>Status</span>
          <select
            value={values.status}
            onChange={(event) => {
              updateValue(
                'status',
                event.target.value as ProjectStatus,
              )
            }}
            disabled={isSubmitting}
          >
            {projectStatuses.map((status) => (
              <option key={status} value={status}>
                {formatLabel(status)}
              </option>
            ))}
          </select>
        </label>

        {isEdit && (
          <label className="task-form-field">
            <span>Manager</span>
            <select
              value={values.managerId}
              onChange={(event) => {
                updateValue('managerId', event.target.value)
              }}
              disabled={isSubmitting}
            >
              {managerOptions.map((manager) => (
                <option
                  key={manager.id}
                  value={manager.id}
                >
                  {manager.name}
                </option>
              ))}
            </select>
          </label>
        )}

        <label className="task-form-field">
          <span>Start date</span>
          <input
            type="date"
            value={values.startDate}
            onChange={(event) => {
              updateValue('startDate', event.target.value)
            }}
            disabled={isSubmitting}
          />
        </label>

        <label className="task-form-field">
          <span>End date</span>
          <input
            type="date"
            value={values.endDate}
            onChange={(event) => {
              updateValue('endDate', event.target.value)
            }}
            disabled={isSubmitting}
          />
        </label>
      </div>

      {!isEdit && (
        <p className="task-form-hint">
          You will become the project owner and initial manager.
        </p>
      )}

      {errorMessage && (
        <p
          className="task-action-message task-action-message--error"
          role="alert"
        >
          {errorMessage}
        </p>
      )}

      <div className="task-form-actions">
        <button
          className="task-action-button task-action-button--primary"
          type="submit"
          disabled={isSubmitting}
        >
          {isSubmitting
            ? 'Saving...'
            : isEdit
              ? 'Save project'
              : 'Create project'}
        </button>
      </div>
    </form>
  )
}
