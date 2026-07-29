import { useEffect, useState } from 'react'
import {
  Link,
  useNavigate,
  useParams,
} from 'react-router'
import { ApiClientError } from '../api/apiClient'
import { getCollaborators } from '../api/collaboratorsApi'
import { getProjects } from '../api/projectsApi'
import {
  completeTask,
  deleteTask,
  getTaskById,
  pauseTaskTimer,
  resumeTaskTimer,
  startTaskTimer,
} from '../api/tasksApi'
import { useAuth } from '../auth/useAuth'
import { AppHeader } from '../components/AppHeader'
import { TaskForm } from '../components/TaskForm'
import type { Collaborator } from '../types/collaborator'
import type { Project } from '../types/project'
import type { Task } from '../types/task'

type TaskAction =
  | 'start'
  | 'pause'
  | 'resume'
  | 'complete'

const dateTimeFormatter = new Intl.DateTimeFormat(
  undefined,
  {
    dateStyle: 'medium',
    timeStyle: 'short',
  },
)

function formatLabel(value: string): string {
  return value
    .toLowerCase()
    .split('_')
    .map((part) => (
      part.charAt(0).toUpperCase() + part.slice(1)
    ))
    .join(' ')
}

function getStatusClassName(status: string): string {
  const normalizedStatus = status
    .toLowerCase()
    .replace(/_/g, '-')

  return `status-badge status-badge--${normalizedStatus}`
}

function getPriorityClassName(priority: string): string {
  return `priority-badge priority-badge--${priority.toLowerCase()}`
}

function formatDateTime(value: string | null): string {
  if (!value) {
    return 'Not set'
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return 'Not set'
  }

  return dateTimeFormatter.format(date)
}

function formatDuration(totalSeconds: number): string {
  const safeSeconds = Math.max(
    0,
    Math.floor(totalSeconds),
  )

  const hours = Math.floor(safeSeconds / 3600)
  const minutes = Math.floor(
    (safeSeconds % 3600) / 60,
  )
  const seconds = safeSeconds % 60

  if (hours > 0) {
    return `${hours}h ${minutes}m ${seconds}s`
  }

  if (minutes > 0) {
    return `${minutes}m ${seconds}s`
  }

  return `${seconds}s`
}

function getDisplayedTime(
  task: Task,
  currentTime: number,
): number {
  if (!task.timerActive || !task.timerStartedAt) {
    return task.totalTimeSeconds
  }

  const startedAt = new Date(
    task.timerStartedAt,
  ).getTime()

  if (Number.isNaN(startedAt)) {
    return task.totalTimeSeconds
  }

  const currentSessionSeconds = Math.max(
    0,
    Math.floor((currentTime - startedAt) / 1000),
  )

  return task.totalTimeSeconds + currentSessionSeconds
}

function getTimerStatus(task: Task): string {
  if (task.timerActive) {
    return 'Running'
  }

  if (task.status === 'COMPLETED') {
    return 'Completed'
  }

  return 'Not running'
}

function resolveProjectName(
  task: Task,
  projects: Project[],
): string {
  if (task.projectId === null) {
    return 'No project'
  }

  return projects.find(
    (project) => project.id === task.projectId,
  )?.name ?? 'Unknown project'
}

function resolveAssigneeName(
  task: Task,
  collaborators: Collaborator[],
): string {
  if (task.assigneeId === null) {
    return 'Not assigned'
  }

  return collaborators.find(
    (collaborator) => (
      collaborator.id === task.assigneeId
    ),
  )?.name ?? 'Unknown collaborator'
}

export function TaskDetailPage() {
  const { session } = useAuth()
  const navigate = useNavigate()
  const { taskId: taskIdParameter } = useParams()

  const taskId = Number(taskIdParameter)
  const hasValidTaskId =
    Number.isSafeInteger(taskId) && taskId > 0

  const [task, setTask] = useState<Task | null>(null)
  const [projects, setProjects] = useState<Project[]>([])
  const [collaborators, setCollaborators] = useState<
    Collaborator[]
  >([])
  const [projectName, setProjectName] =
    useState('No project')
  const [assigneeName, setAssigneeName] =
    useState('Not assigned')

  const [isLoading, setIsLoading] = useState(true)
  const [isNotFound, setIsNotFound] = useState(false)
  const [errorMessage, setErrorMessage] =
    useState<string | null>(null)

  const [reloadVersion, setReloadVersion] = useState(0)
  const [currentTime, setCurrentTime] = useState(
    () => Date.now(),
  )

  const [activeAction, setActiveAction] =
    useState<TaskAction | null>(null)
  const [isEditing, setIsEditing] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)

  const [actionMessage, setActionMessage] =
    useState<string | null>(null)

  const [actionError, setActionError] =
    useState<string | null>(null)

  useEffect(() => {
    if (!session || !hasValidTaskId) {
      return
    }

    const currentSession = session
    const controller = new AbortController()

    async function loadTaskDetail() {
      setIsLoading(true)
      setIsNotFound(false)
      setErrorMessage(null)
      setActionMessage(null)
      setActionError(null)

      try {
        const [
          taskResponse,
          projectResponse,
          collaboratorResponse,
        ] = await Promise.all([
          getTaskById(
            taskId,
            currentSession,
            controller.signal,
          ),
          getProjects(
            currentSession,
            controller.signal,
          ),
          getCollaborators(
            currentSession,
            controller.signal,
          ),
        ])

        setTask(taskResponse)
        setProjects(projectResponse)
        setCollaborators(collaboratorResponse)
        setProjectName(
          resolveProjectName(
            taskResponse,
            projectResponse,
          ),
        )
        setAssigneeName(
          resolveAssigneeName(
            taskResponse,
            collaboratorResponse,
          ),
        )
        setCurrentTime(Date.now())
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

        if (
          error instanceof ApiClientError &&
          error.status === 404
        ) {
          setTask(null)
          setIsNotFound(true)
          return
        }

        setErrorMessage(
          error instanceof Error
            ? error.message
            : 'The task detail could not be loaded.',
        )
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false)
        }
      }
    }

    void loadTaskDetail()

    return () => {
      controller.abort()
    }
  }, [
    hasValidTaskId,
    reloadVersion,
    session,
    taskId,
  ])

  useEffect(() => {
    if (
      !task?.timerActive ||
      task.timerStartedAt === null
    ) {
      return
    }

    const intervalId = window.setInterval(() => {
      setCurrentTime(Date.now())
    }, 1000)

    return () => {
      window.clearInterval(intervalId)
    }
  }, [task])

  if (!session) {
    return null
  }

  function handleRetry() {
    setReloadVersion((current) => current + 1)
  }

  function handleTaskSaved(updatedTask: Task) {
    setTask(updatedTask)
    setProjectName(
      resolveProjectName(updatedTask, projects),
    )
    setAssigneeName(
      resolveAssigneeName(
        updatedTask,
        collaborators,
      ),
    )
    setCurrentTime(Date.now())
    setIsEditing(false)
    setActionError(null)
    setActionMessage('The task was updated.')
  }

  async function handleDelete(): Promise<void> {
    if (!task || !session || isDeleting) {
      return
    }

    const confirmed = window.confirm(
      `Delete task #${task.id}? This action cannot be undone.`,
    )

    if (!confirmed) {
      return
    }

    setIsDeleting(true)
    setActionMessage(null)
    setActionError(null)

    try {
      await deleteTask(task.id, session)
      navigate('/tasks', { replace: true })
    } catch (error) {
      if (
        error instanceof ApiClientError &&
        error.status === 401
      ) {
        return
      }

      setActionError(
        error instanceof Error
          ? error.message
          : 'The task could not be deleted.',
      )
    } finally {
      setIsDeleting(false)
    }
  }

  async function runTaskAction(
    action: TaskAction,
  ): Promise<void> {
    if (!session || !task) {
      return
    }

    setActiveAction(action)
    setActionMessage(null)
    setActionError(null)

    try {
      let updatedTask: Task

      switch (action) {
        case 'start':
          updatedTask = await startTaskTimer(
            task.id,
            session,
          )
          break

        case 'pause':
          updatedTask = await pauseTaskTimer(
            task.id,
            session,
          )
          break

        case 'resume':
          updatedTask = await resumeTaskTimer(
            task.id,
            session,
          )
          break

        case 'complete':
          updatedTask = await completeTask(
            task.id,
            session,
          )
          break
      }

      setTask(updatedTask)
      setCurrentTime(Date.now())

      const successMessages: Record<
        TaskAction,
        string
      > = {
        start: 'The timer was started.',
        pause: 'The timer was paused.',
        resume: 'The timer was resumed.',
        complete: 'The task was completed.',
      }

      setActionMessage(successMessages[action])
    } catch (error) {
      if (
        error instanceof ApiClientError &&
        error.status === 401
      ) {
        return
      }

      setActionError(
        error instanceof Error
          ? error.message
          : 'The task could not be updated.',
      )
    } finally {
      setActiveAction(null)
    }
  }

  function handleStartOrResume(): void {
    if (!task) {
      return
    }

    const shouldResume =
      task.status === 'IN_PROGRESS' ||
      task.totalTimeSeconds > 0

    void runTaskAction(
      shouldResume ? 'resume' : 'start',
    )
  }

  function handlePause(): void {
    void runTaskAction('pause')
  }

  function handleComplete(): void {
    if (!task) {
      return
    }

    const confirmed = window.confirm(
      'Complete this task? A completed task cannot restart its timer.',
    )

    if (!confirmed) {
      return
    }

    void runTaskAction('complete')
  }

  const isActionRunning =
    activeAction !== null || isDeleting

  const shouldResume =
    task !== null &&
    !task.timerActive &&
    task.status !== 'COMPLETED' &&
    (
      task.status === 'IN_PROGRESS' ||
      task.totalTimeSeconds > 0
    )

  return (
    <main className="page">
      <section className="card card--wide">
        <AppHeader
          title={task?.title ?? 'Task detail'}
          description="Review and manage task information, assignment and tracked time."
        />

        <div
          className="task-detail-content"
          aria-live="polite"
        >
          <Link
            className="task-back-link"
            to="/tasks"
          >
            Back to tasks
          </Link>

          {hasValidTaskId && isLoading && (
            <section className="dashboard-state">
              <h2>Loading task</h2>
              <p>
                Retrieving the task, project and assignee.
              </p>
            </section>
          )}

          {(!hasValidTaskId ||
            (!isLoading && isNotFound)) && (
            <section
              className="dashboard-state dashboard-state--error"
              role="alert"
            >
              <h2>Task not found</h2>
              <p>
                The requested task does not exist or is no
                longer available.
              </p>
            </section>
          )}

          {hasValidTaskId &&
            !isLoading &&
            !isNotFound &&
            errorMessage && (
              <section
                className="dashboard-state dashboard-state--error"
                role="alert"
              >
                <h2>Task unavailable</h2>
                <p>{errorMessage}</p>

                <button
                  className="secondary-button"
                  type="button"
                  onClick={handleRetry}
                >
                  Try again
                </button>
              </section>
            )}

          {hasValidTaskId &&
            !isLoading &&
            !isNotFound &&
            !errorMessage &&
            task && (
              <>
                <section className="task-management-toolbar">
                  <div>
                    <p className="eyebrow">
                      Task management
                    </p>
                    <p>
                      Edit the task or remove it from the
                      workspace.
                    </p>
                  </div>

                  <div className="task-action-buttons">
                    <button
                      className="task-action-button task-action-button--secondary"
                      type="button"
                      disabled={isActionRunning}
                      onClick={() => {
                        setIsEditing((current) => !current)
                        setActionMessage(null)
                        setActionError(null)
                      }}
                    >
                      {isEditing
                        ? 'Close editor'
                        : 'Edit task'}
                    </button>

                    <button
                      className="task-action-button task-action-button--danger"
                      type="button"
                      disabled={isActionRunning}
                      onClick={() => {
                        void handleDelete()
                      }}
                    >
                      {isDeleting
                        ? 'Deleting…'
                        : 'Delete task'}
                    </button>
                  </div>
                </section>

                {isEditing && (
                  <TaskForm
                    mode="edit"
                    session={session}
                    projects={projects}
                    initialTask={task}
                    onSaved={handleTaskSaved}
                    onCancel={() => {
                      setIsEditing(false)
                    }}
                  />
                )}

                <section className="task-detail-overview">
                  <header className="task-detail-heading">
                    <div>
                      <span className="task-reference">
                        Task #{task.id}
                      </span>

                      <h2>{task.title}</h2>
                    </div>

                    <div className="task-card-badges">
                      <span
                        className={getStatusClassName(
                          task.status,
                        )}
                      >
                        {formatLabel(task.status)}
                      </span>

                      <span
                        className={getPriorityClassName(
                          task.priority,
                        )}
                      >
                        {formatLabel(task.priority)}
                      </span>
                    </div>
                  </header>

                  <p className="task-detail-description">
                    {task.description?.trim() ||
                      'No description has been added.'}
                  </p>

                  {task.timerActive && (
                    <div className="timer-indicator">
                      <span
                        className="timer-indicator-dot"
                        aria-hidden="true"
                      />
                      Timer currently running
                    </div>
                  )}

                  <dl className="task-detail-metadata">
                    <div>
                      <dt>Project</dt>
                      <dd>
                        {task.projectId === null ? (
                          projectName
                        ) : (
                          <Link
                            className="task-relation-link"
                            to={`/projects/${task.projectId}`}
                          >
                            {projectName}
                          </Link>
                        )}
                      </dd>
                    </div>

                    <div>
                      <dt>Assignee</dt>
                      <dd>{assigneeName}</dd>
                    </div>

                    <div>
                      <dt>Tracked time</dt>
                      <dd>
                        {formatDuration(
                          getDisplayedTime(
                            task,
                            currentTime,
                          ),
                        )}
                      </dd>
                    </div>

                    <div>
                      <dt>Timer status</dt>
                      <dd>{getTimerStatus(task)}</dd>
                    </div>

                    <div>
                      <dt>Timer started</dt>
                      <dd>
                        {formatDateTime(
                          task.timerStartedAt,
                        )}
                      </dd>
                    </div>

                    <div>
                      <dt>Created</dt>
                      <dd>
                        {formatDateTime(task.createdAt)}
                      </dd>
                    </div>

                    <div>
                      <dt>Last updated</dt>
                      <dd>
                        {formatDateTime(task.updatedAt)}
                      </dd>
                    </div>
                  </dl>

                  <section
                    className="task-timer-controls"
                    aria-busy={isActionRunning}
                  >
                    <div className="task-timer-controls-heading">
                      <div>
                        <p className="eyebrow">
                          Timer workflow
                        </p>

                        <h3>Task controls</h3>
                      </div>

                      <p>
                        Start, pause or complete the current
                        task.
                      </p>
                    </div>

                    {task.status === 'COMPLETED' ? (
                      <p className="task-completed-message">
                        This task is completed and its timer
                        cannot be restarted.
                      </p>
                    ) : (
                      <div className="task-action-buttons">
                        {task.timerActive ? (
                          <button
                            className="task-action-button task-action-button--secondary"
                            type="button"
                            disabled={isActionRunning}
                            onClick={handlePause}
                          >
                            {activeAction === 'pause'
                              ? 'Pausing...'
                              : 'Pause timer'}
                          </button>
                        ) : (
                          <button
                            className="task-action-button task-action-button--primary"
                            type="button"
                            disabled={isActionRunning}
                            onClick={handleStartOrResume}
                          >
                            {activeAction === 'start'
                              ? 'Starting...'
                              : activeAction === 'resume'
                                ? 'Resuming...'
                                : shouldResume
                                  ? 'Resume timer'
                                  : 'Start timer'}
                          </button>
                        )}

                        <button
                          className="task-action-button task-action-button--danger"
                          type="button"
                          disabled={isActionRunning}
                          onClick={handleComplete}
                        >
                          {activeAction === 'complete'
                            ? 'Completing...'
                            : 'Complete task'}
                        </button>
                      </div>
                    )}

                    {actionMessage && (
                      <p
                        className="task-action-message task-action-message--success"
                        role="status"
                      >
                        {actionMessage}
                      </p>
                    )}

                    {actionError && (
                      <p
                        className="task-action-message task-action-message--error"
                        role="alert"
                      >
                        {actionError}
                      </p>
                    )}
                  </section>
                </section>
              </>
            )}
        </div>
      </section>
    </main>
  )
}
