import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { ApiClientError } from '../api/apiClient'
import { getCollaborators } from '../api/collaboratorsApi'
import { getProjects } from '../api/projectsApi'
import { getTasks } from '../api/tasksApi'
import { useAuth } from '../auth/useAuth'
import { AppHeader } from '../components/AppHeader'
import type { TaskListItem } from '../types/task'

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
    return `${hours}h ${minutes}m`
  }

  if (minutes > 0) {
    return `${minutes}m ${seconds}s`
  }

  return `${seconds}s`
}

function getDisplayedTime(
  task: TaskListItem,
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

export function TasksPage() {
  const { session } = useAuth()

  const [tasks, setTasks] = useState<TaskListItem[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] =
    useState<string | null>(null)
  const [reloadVersion, setReloadVersion] = useState(0)
  const [currentTime, setCurrentTime] = useState(
    () => Date.now(),
  )

  useEffect(() => {
    if (!session) {
      return
    }

    const currentSession = session
    const controller = new AbortController()

    async function loadTasks() {
      setIsLoading(true)
      setErrorMessage(null)

      try {
        const [
          taskResponse,
          projectResponse,
          collaboratorResponse,
        ] = await Promise.all([
          getTasks(
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

        const projectNames = new Map(
          projectResponse.map((project) => [
            project.id,
            project.name,
          ]),
        )

        const collaboratorNames = new Map(
          collaboratorResponse.map((collaborator) => [
            collaborator.id,
            collaborator.name,
          ]),
        )

        const taskItems = taskResponse.map(
          (task): TaskListItem => ({
            ...task,
            projectName:
              task.projectId === null
                ? 'No project'
                : projectNames.get(task.projectId) ??
                  'Unknown project',
            assigneeName:
              task.assigneeId === null
                ? 'Not assigned'
                : collaboratorNames.get(
                    task.assigneeId,
                  ) ?? 'Unknown collaborator',
          }),
        )

        setTasks(taskItems)
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

        setErrorMessage(
          error instanceof Error
            ? error.message
            : 'The tasks could not be loaded.',
        )
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false)
        }
      }
    }

    void loadTasks()

    return () => {
      controller.abort()
    }
  }, [session, reloadVersion])

  useEffect(() => {
    const hasActiveTimer = tasks.some(
      (task) => (
        task.timerActive &&
        task.timerStartedAt !== null
      ),
    )

    if (!hasActiveTimer) {
      return
    }

    const intervalId = window.setInterval(() => {
      setCurrentTime(Date.now())
    }, 1000)

    return () => {
      window.clearInterval(intervalId)
    }
  }, [tasks])

  if (!session) {
    return null
  }

  const activeTimerCount = tasks.filter(
    (task) => task.timerActive,
  ).length

  function handleRetry() {
    setReloadVersion((current) => current + 1)
  }

  return (
    <main className="page">
      <section className="card card--wide">
        <AppHeader
          title="Tasks"
          description="Review tasks, assignments, priorities and tracked time."
        />

        <div
          className="tasks-content"
          aria-live="polite"
        >
          {isLoading && (
            <section className="dashboard-state">
              <h2>Loading tasks</h2>
              <p>
                Retrieving tasks, projects and assignees.
              </p>
            </section>
          )}

          {!isLoading && errorMessage && (
            <section
              className="dashboard-state dashboard-state--error"
              role="alert"
            >
              <h2>Tasks unavailable</h2>
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

          {!isLoading &&
            !errorMessage &&
            tasks.length === 0 && (
              <section className="dashboard-state">
                <h2>No tasks found</h2>
                <p>
                  Tasks created in the workspace will
                  appear here.
                </p>
              </section>
            )}

          {!isLoading &&
            !errorMessage &&
            tasks.length > 0 && (
              <>
                <div className="tasks-summary">
                  <p>
                    <strong>{tasks.length}</strong>{' '}
                    {tasks.length === 1 ? 'task' : 'tasks'}
                  </p>

                  <p>
                    <strong>{activeTimerCount}</strong>{' '}
                    active{' '}
                    {activeTimerCount === 1
                      ? 'timer'
                      : 'timers'}
                  </p>
                </div>

                <div className="tasks-grid">
                  {tasks.map((task) => (
                    <article
                      className="task-card"
                      key={task.id}
                    >
                      <header className="task-card-header">
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

                      <p className="task-description">
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

                      <dl className="task-details">
                        <div>
                          <dt>Project</dt>
                          <dd>{task.projectName}</dd>
                        </div>

                        <div>
                          <dt>Assignee</dt>
                          <dd>{task.assigneeName}</dd>
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
                          <dt>Last updated</dt>
                          <dd>
                            {formatDateTime(task.updatedAt)}
                          </dd>
                        </div>
                      </dl>

                      <footer className="task-card-footer">
                        <span>
                          Created{' '}
                          {formatDateTime(task.createdAt)}
                        </span>

                        <Link
                          className="task-detail-link"
                          to={`/tasks/${task.id}`}
                        >
                          View task
                        </Link>
                      </footer>
                    </article>
                  ))}
                </div>
              </>
            )}
        </div>
      </section>
    </main>
  )
}