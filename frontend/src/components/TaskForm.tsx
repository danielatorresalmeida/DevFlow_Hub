import {
  useEffect,
  useMemo,
  useState,
} from 'react'
import { ApiClientError } from '../api/apiClient'
import { getProjectMembers } from '../api/projectMembershipsApi'
import {
  createTask,
  updateTask,
} from '../api/tasksApi'
import type { AuthSession } from '../types/auth'
import type { Project } from '../types/project'
import type { ProjectMember } from '../types/projectMembership'
import type {
  Task,
  TaskInput,
  TaskPriority,
  TaskStatus,
} from '../types/task'

type TaskFormMode = 'create' | 'edit'

interface TaskFormProps {
  mode: TaskFormMode
  session: AuthSession
  projects: Project[]
  initialTask?: Task
  onSaved: (task: Task) => void
  onCancel: () => void
}

interface TaskFormValues {
  title: string
  description: string
  status: TaskStatus
  priority: TaskPriority
  projectId: string
  assigneeId: string
}

const taskStatuses: TaskStatus[] = [
  'PENDING',
  'IN_PROGRESS',
  'REVIEW',
  'COMPLETED',
]

const taskPriorities: TaskPriority[] = [
  'LOW',
  'MEDIUM',
  'HIGH',
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
  initialTask?: Task,
): TaskFormValues {
  if (initialTask) {
    return {
      title: initialTask.title,
      description: initialTask.description ?? '',
      status: initialTask.status,
      priority: initialTask.priority,
      projectId:
        initialTask.projectId?.toString() ?? '',
      assigneeId:
        initialTask.assigneeId?.toString() ?? '',
    }
  }

  return {
    title: '',
    description: '',
    status: 'PENDING',
    priority: 'MEDIUM',
    projectId: '',
    assigneeId: session.collaborator.id.toString(),
  }
}

function getErrorMessage(error: unknown): string {
  if (error instanceof Error) {
    return error.message
  }

  return 'The task could not be saved.'
}

export function TaskForm({
  mode,
  session,
  projects,
  initialTask,
  onSaved,
  onCancel,
}: TaskFormProps) {
  const [values, setValues] = useState(
    () => createInitialValues(session, initialTask),
  )
  const [projectMembers, setProjectMembers] =
    useState<ProjectMember[]>([])
  const [isLoadingMembers, setIsLoadingMembers] =
    useState(false)
  const [membersError, setMembersError] =
    useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] =
    useState(false)
  const [formError, setFormError] =
    useState<string | null>(null)
  const [validationErrors, setValidationErrors] =
    useState<Record<string, string>>({})

  const selectedProjectId = values.projectId
    ? Number(values.projectId)
    : null

  useEffect(() => {
    if (selectedProjectId === null) {
      return
    }

    const projectId = selectedProjectId
    const controller = new AbortController()

    async function loadProjectMembers() {
      setIsLoadingMembers(true)
      setMembersError(null)

      try {
        const members = await getProjectMembers(
          projectId,
          session,
          controller.signal,
        )

        setProjectMembers(members)

        setValues((current) => {
          const currentUserMembership = members.find(
            (member) => (
              member.collaboratorId ===
              session.collaborator.id
            ),
          )

          if (
            currentUserMembership?.role ===
            'CONTRIBUTOR'
          ) {
            return {
              ...current,
              assigneeId:
                currentUserMembership.collaboratorId
                  .toString(),
            }
          }

          const currentAssigneeExists = members.some(
            (member) => (
              member.collaboratorId.toString() ===
              current.assigneeId
            ),
          )

          if (currentAssigneeExists) {
            return current
          }

          return {
            ...current,
            assigneeId:
              currentUserMembership?.collaboratorId
                .toString() ?? '',
          }
        })
      } catch (error) {
        if (controller.signal.aborted) {
          return
        }

        if (
          error instanceof ApiClientError &&
          error.status === 401
        ) {
          return
        }

        setProjectMembers([])
        setMembersError(
          error instanceof Error
            ? error.message
            : 'Project members could not be loaded.',
        )
      } finally {
        if (!controller.signal.aborted) {
          setIsLoadingMembers(false)
        }
      }
    }

    void loadProjectMembers()

    return () => {
      controller.abort()
    }
  }, [selectedProjectId, session])

  const currentMembership = useMemo(
    () => projectMembers.find(
      (member) => (
        member.collaboratorId ===
        session.collaborator.id
      ),
    ),
    [projectMembers, session.collaborator.id],
  )

  const assignableMembers = useMemo(
    () => (
      currentMembership?.role === 'CONTRIBUTOR'
        ? projectMembers.filter(
            (member) => (
              member.collaboratorId ===
              session.collaborator.id
            ),
          )
        : projectMembers
    ),
    [
      currentMembership?.role,
      projectMembers,
      session.collaborator.id,
    ],
  )

  const canSubmitForSelectedProject =
    selectedProjectId === null ||
    (
      currentMembership !== undefined &&
      currentMembership.role !== 'VIEWER'
    )

  const isStatusLocked =
    mode === 'edit' &&
    initialTask?.timerActive === true

  const submitDisabled =
    isSubmitting ||
    isLoadingMembers ||
    values.title.trim().length === 0 ||
    !canSubmitForSelectedProject

  function updateValue(
    field: keyof TaskFormValues,
    value: string,
  ) {
    setValues((current) => ({
      ...current,
      [field]: value,
    }))

    setValidationErrors((current) => {
      if (!(field in current)) {
        return current
      }

      const next = { ...current }
      delete next[field]
      return next
    })

    setFormError(null)
  }

  function buildInput(): TaskInput {
    return {
      title: values.title.trim(),
      description:
        values.description.trim().length > 0
          ? values.description.trim()
          : null,
      status: values.status,
      priority: values.priority,
      projectId: selectedProjectId,
      assigneeId: values.assigneeId
        ? Number(values.assigneeId)
        : null,
    }
  }

  async function handleSubmit(
    event: React.FormEvent<HTMLFormElement>,
  ) {
    event.preventDefault()

    if (submitDisabled) {
      return
    }

    setIsSubmitting(true)
    setFormError(null)
    setValidationErrors({})

    try {
      const input = buildInput()

      const savedTask = mode === 'create'
        ? await createTask(input, session)
        : await updateTask(
            initialTask?.id ?? 0,
            input,
            session,
          )

      onSaved(savedTask)
    } catch (error) {
      if (
        error instanceof ApiClientError &&
        error.status === 401
      ) {
        return
      }

      if (error instanceof ApiClientError) {
        setValidationErrors(error.validationErrors)
      }

      setFormError(getErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className="task-form-panel">
      <div className="task-form-heading">
        <div>
          <p className="eyebrow">
            {mode === 'create'
              ? 'New task'
              : 'Task management'}
          </p>
          <h2>
            {mode === 'create'
              ? 'Create task'
              : 'Edit task'}
          </h2>
        </div>

        <p>
          {mode === 'create'
            ? 'Create a personal task or assign work inside an accessible project.'
            : 'Update the task details, assignment, priority and status.'}
        </p>
      </div>

      <form
        className="task-management-form"
        onSubmit={handleSubmit}
        aria-busy={isSubmitting}
      >
        <div className="task-form-grid">
          <label className="task-form-field task-form-field--wide">
            <span>Title</span>
            <input
              type="text"
              maxLength={150}
              value={values.title}
              onChange={(event) => {
                updateValue('title', event.target.value)
              }}
              aria-invalid={Boolean(validationErrors.title)}
              required
            />
            {validationErrors.title && (
              <span className="field-error">
                {validationErrors.title}
              </span>
            )}
          </label>

          <label className="task-form-field">
            <span>Project</span>
            <select
              value={values.projectId}
              onChange={(event) => {
                const projectId = event.target.value

                setProjectMembers([])
                setMembersError(null)
                setValues((current) => ({
                  ...current,
                  projectId,
                  assigneeId: projectId
                    ? ''
                    : session.collaborator.id.toString(),
                }))
                setFormError(null)
              }}
            >
              <option value="">
                No project (personal task)
              </option>
              {projects.map((project) => (
                <option
                  key={project.id}
                  value={project.id}
                >
                  {project.name}
                </option>
              ))}
            </select>
          </label>

          <label className="task-form-field">
            <span>Assignee</span>
            {selectedProjectId === null ? (
              <input
                type="text"
                value={session.collaborator.name}
                disabled
              />
            ) : (
              <select
                value={values.assigneeId}
                onChange={(event) => {
                  updateValue(
                    'assigneeId',
                    event.target.value,
                  )
                }}
                disabled={
                  isLoadingMembers ||
                  membersError !== null ||
                  currentMembership?.role ===
                    'CONTRIBUTOR'
                }
              >
                <option value="">
                  {isLoadingMembers
                    ? 'Loading project members…'
                    : 'Not assigned'}
                </option>
                {assignableMembers.map((member) => (
                  <option
                    key={member.collaboratorId}
                    value={member.collaboratorId}
                  >
                    {member.collaboratorName} ·{' '}
                    {formatLabel(member.role)}
                  </option>
                ))}
              </select>
            )}
          </label>

          <label className="task-form-field">
            <span>Status</span>
            <select
              value={values.status}
              onChange={(event) => {
                updateValue(
                  'status',
                  event.target.value as TaskStatus,
                )
              }}
              disabled={isStatusLocked}
            >
              {taskStatuses.map((status) => (
                <option key={status} value={status}>
                  {formatLabel(status)}
                </option>
              ))}
            </select>
          </label>

          <label className="task-form-field">
            <span>Priority</span>
            <select
              value={values.priority}
              onChange={(event) => {
                updateValue(
                  'priority',
                  event.target.value as TaskPriority,
                )
              }}
            >
              {taskPriorities.map((priority) => (
                <option key={priority} value={priority}>
                  {formatLabel(priority)}
                </option>
              ))}
            </select>
          </label>

          <label className="task-form-field task-form-field--wide">
            <span>Description</span>
            <textarea
              maxLength={3000}
              rows={5}
              value={values.description}
              onChange={(event) => {
                updateValue(
                  'description',
                  event.target.value,
                )
              }}
              aria-invalid={Boolean(
                validationErrors.description,
              )}
            />
            {validationErrors.description && (
              <span className="field-error">
                {validationErrors.description}
              </span>
            )}
          </label>
        </div>

        {membersError && (
          <p
            className="task-action-message task-action-message--error"
            role="alert"
          >
            {membersError}
          </p>
        )}

        {selectedProjectId !== null &&
          !isLoadingMembers &&
          currentMembership?.role === 'VIEWER' && (
            <p
              className="task-action-message task-action-message--error"
              role="alert"
            >
              Your Viewer membership allows consultation only.
            </p>
          )}

        {isStatusLocked && (
          <p className="task-form-note">
            Pause the active timer before changing the task status.
          </p>
        )}

        {formError && (
          <p
            className="task-action-message task-action-message--error"
            role="alert"
          >
            {formError}
          </p>
        )}

        <div className="task-action-buttons">
          <button
            className="task-action-button task-action-button--primary"
            type="submit"
            disabled={submitDisabled}
          >
            {isSubmitting
              ? 'Saving…'
              : mode === 'create'
                ? 'Create task'
                : 'Save task'}
          </button>

          <button
            className="task-action-button task-action-button--secondary"
            type="button"
            onClick={onCancel}
            disabled={isSubmitting}
          >
            Cancel
          </button>
        </div>
      </form>
    </section>
  )
}
