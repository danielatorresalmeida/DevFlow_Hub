import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { ApiClientError } from '../api/apiClient'
import { getCollaborators } from '../api/collaboratorsApi'
import { getProjectById } from '../api/projectsApi'
import { getTasks } from '../api/tasksApi'
import { useAuth } from '../auth/useAuth'
import { AppHeader } from '../components/AppHeader'
import type { Project } from '../types/project'
import type { Task } from '../types/task'

interface ProjectTaskItem extends Task {
  assigneeName: string
}

const dateFormatter = new Intl.DateTimeFormat(undefined, {
  dateStyle: 'medium',
})

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

function formatDate(value: string | null): string {
  if (!value) {
    return 'Not set'
  }

  const date = new Date(`${value}T00:00:00`)

  if (Number.isNaN(date.getTime())) {
    return 'Not set'
  }

  return dateFormatter.format(date)
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

export function ProjectDetailPage() {
  const { session } = useAuth()
  const { projectId: projectIdParameter } = useParams()

  const projectId = Number(projectIdParameter)
  const hasValidProjectId =
    Number.isSafeInteger(projectId) && projectId > 0

  const [project, setProject] =
    useState<Project | null>(null)

  const [managerName, setManagerName] =
    useState('Not assigned')

  const [projectTasks, setProjectTasks] =
    useState<ProjectTaskItem[]>([])

  const [isLoading, setIsLoading] = useState(true)
  const [isNotFound, setIsNotFound] = useState(false)
  const [errorMessage, setErrorMessage] =
    useState<string | null>(null)

  const [reloadVersion, setReloadVersion] = useState(0)

  useEffect(() => {
    if (!session) {
      return
    }

    if (!hasValidProjectId) {
      return
    }

    const currentSession = session
    const controller = new AbortController()

    async function loadProjectDetail() {
      setIsLoading(true)
      setIsNotFound(false)
      setErrorMessage(null)

      try {
        const [
          projectResponse,
          collaboratorResponse,
          taskResponse,
        ] = await Promise.all([
          getProjectById(
            projectId,
            currentSession,
            controller.signal,
          ),
          getCollaborators(
            currentSession,
            controller.signal,
          ),
          getTasks(
            currentSession,
            controller.signal,
          ),
        ])

        const collaboratorNames = new Map(
          collaboratorResponse.map((collaborator) => [
            collaborator.id,
            collaborator.name,
          ]),
        )

        const resolvedManagerName =
          projectResponse.managerId === null
            ? 'Not assigned'
            : collaboratorNames.get(
                projectResponse.managerId,
              ) ?? 'Unknown collaborator'

        const associatedTasks = taskResponse
          .filter((task) => (
            task.projectId === projectResponse.id
          ))
          .map(
            (task): ProjectTaskItem => ({
              ...task,
              assigneeName:
                task.assigneeId === null
                  ? 'Not assigned'
                  : collaboratorNames.get(
                      task.assigneeId,
                    ) ?? 'Unknown collaborator',
            }),
          )

        setProject(projectResponse)
        setManagerName(resolvedManagerName)
        setProjectTasks(associatedTasks)
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
          setProject(null)
          setProjectTasks([])
          setIsNotFound(true)
          return
        }

        setErrorMessage(
          error instanceof Error
            ? error.message
            : 'The project detail could not be loaded.',
        )
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false)
        }
      }
    }

    void loadProjectDetail()

    return () => {
      controller.abort()
    }
  }, [
    hasValidProjectId,
    projectId,
    reloadVersion,
    session,
  ])

  if (!session) {
    return null
  }

  function handleRetry() {
    setReloadVersion((current) => current + 1)
  }

  return (
    <main className="page">
      <section className="card card--wide">
        <AppHeader
          title={project?.name ?? 'Project detail'}
          description="Review project information and its associated tasks."
        />

        <div
          className="project-detail-content"
          aria-live="polite"
        >
          <Link
            className="project-back-link"
            to="/projects"
          >
            Back to projects
          </Link>

          {hasValidProjectId && isLoading && (
            <section className="dashboard-state">
              <h2>Loading project</h2>
              <p>
                Retrieving the project, manager and tasks.
              </p>
            </section>
          )}

          {(!hasValidProjectId ||
            (!isLoading && isNotFound)) && (
            <section
              className="dashboard-state dashboard-state--error"
              role="alert"
            >
              <h2>Project not found</h2>
              <p>
                The requested project does not exist or is
                no longer available.
              </p>
            </section>
          )}

          {hasValidProjectId &&
            !isLoading &&
            !isNotFound &&
            errorMessage && (
              <section
                className="dashboard-state dashboard-state--error"
                role="alert"
              >
                <h2>Project unavailable</h2>
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

          {hasValidProjectId &&
            !isLoading &&
            !isNotFound &&
            !errorMessage &&
            project && (
              <>
                <section className="project-detail-overview">
                  <header className="project-detail-heading">
                    <div>
                      <span className="project-reference">
                        Project #{project.id}
                      </span>

                      <h2>{project.name}</h2>
                    </div>

                    <span
                      className={getStatusClassName(
                        project.status,
                      )}
                    >
                      {formatLabel(project.status)}
                    </span>
                  </header>

                  <p className="project-detail-description">
                    {project.description?.trim() ||
                      'No description has been added.'}
                  </p>

                  <dl className="project-detail-metadata">
                    <div>
                      <dt>Manager</dt>
                      <dd>{managerName}</dd>
                    </div>

                    <div>
                      <dt>Start date</dt>
                      <dd>
                        {formatDate(project.startDate)}
                      </dd>
                    </div>

                    <div>
                      <dt>End date</dt>
                      <dd>
                        {formatDate(project.endDate)}
                      </dd>
                    </div>

                    <div>
                      <dt>Associated tasks</dt>
                      <dd>{projectTasks.length}</dd>
                    </div>
                  </dl>
                </section>

                <section className="project-detail-tasks">
                  <div className="project-detail-section-heading">
                    <div>
                      <p className="eyebrow">
                        Project work
                      </p>
                      <h2>Associated tasks</h2>
                    </div>

                    <span className="project-task-count">
                      {projectTasks.length}{' '}
                      {projectTasks.length === 1
                        ? 'task'
                        : 'tasks'}
                    </span>
                  </div>

                  {projectTasks.length === 0 && (
                    <section className="dashboard-state">
                      <h3>No associated tasks</h3>
                      <p>
                        Tasks assigned to this project will
                        appear here.
                      </p>
                    </section>
                  )}

                  {projectTasks.length > 0 && (
                    <div className="project-task-list">
                      {projectTasks.map((task) => (
                        <article
                          className="project-task-item"
                          key={task.id}
                        >
                          <header className="project-task-header">
                            <div>
                              <span className="task-reference">
                                Task #{task.id}
                              </span>

                              <h3>{task.title}</h3>
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

                          <p className="project-task-description">
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

                          <dl className="project-task-metadata">
                            <div>
                              <dt>Assignee</dt>
                              <dd>{task.assigneeName}</dd>
                            </div>

                            <div>
                              <dt>Tracked time</dt>
                              <dd>
                                {formatDuration(
                                  task.totalTimeSeconds,
                                )}
                              </dd>
                            </div>

                            <div>
                              <dt>Last updated</dt>
                              <dd>
                                {formatDateTime(
                                  task.updatedAt,
                                )}
                              </dd>
                            </div>
                          </dl>
                        </article>
                      ))}
                    </div>
                  )}
                </section>
              </>
            )}
        </div>
      </section>
    </main>
  )
}