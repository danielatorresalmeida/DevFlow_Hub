import { useEffect, useState } from 'react'

import {
  ApiClientError,
} from '../api/apiClient'
import { getDashboardSummary } from '../api/dashboardApi'
import { useAuth } from '../auth/useAuth'
import { AppHeader } from '../components/AppHeader'
import type {
  DashboardProjectItem,
  DashboardSummary,
  DashboardTaskItem,
} from '../types/dashboard'

const dateFormatter = new Intl.DateTimeFormat(undefined, {
  dateStyle: 'medium',
})

const dateTimeFormatter = new Intl.DateTimeFormat(undefined, {
  dateStyle: 'medium',
  timeStyle: 'short',
})

function formatStatus(value: string): string {
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

function formatTrackedTime(totalSeconds: number): string {
  if (totalSeconds < 60) {
    return `${totalSeconds}s`
  }

  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor(
    (totalSeconds % 3600) / 60,
  )

  if (hours === 0) {
    return `${minutes}m`
  }

  return `${hours}h ${minutes}m`
}

function formatDate(value: string): string {
  const [year, month, day] = value
    .split('-')
    .map(Number)

  return dateFormatter.format(
    new Date(year, month - 1, day),
  )
}

function formatDateTime(value: string | null): string {
  if (!value) {
    return 'No activity recorded'
  }

  return dateTimeFormatter.format(new Date(value))
}

function RecentTask({ task }: {
  task: DashboardTaskItem
}) {
  return (
    <li className="dashboard-list-item">
      <div className="dashboard-list-main">
        <strong>{task.title}</strong>
        <span>Assigned to {task.assigneeName}</span>
      </div>

      <div className="dashboard-list-meta">
        <span className={getStatusClassName(task.status)}>
          {formatStatus(task.status)}
        </span>

        <span>{formatStatus(task.priority)}</span>

        <time dateTime={task.updatedAt ?? undefined}>
          {formatDateTime(task.updatedAt)}
        </time>
      </div>
    </li>
  )
}

function UpcomingProject({ project }: {
  project: DashboardProjectItem
}) {
  return (
    <li className="dashboard-list-item">
      <div className="dashboard-list-main">
        <strong>{project.name}</strong>
        <span>Managed by {project.managerName}</span>
      </div>

      <div className="dashboard-list-meta">
        <span className={getStatusClassName(project.status)}>
          {formatStatus(project.status)}
        </span>

        <time dateTime={project.endDate}>
          Due {formatDate(project.endDate)}
        </time>
      </div>
    </li>
  )
}

export function DashboardPage() {
  const { session } = useAuth()

  const [summary, setSummary] =
    useState<DashboardSummary | null>(null)

  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] =
    useState<string | null>(null)

  const [reloadVersion, setReloadVersion] = useState(0)

  useEffect(() => {
    if (!session) {
      return
    }

    const currentSession = session
    const controller = new AbortController()

    async function loadDashboard() {
      setIsLoading(true)
      setErrorMessage(null)
      setSummary(null)

      try {
        const response = await getDashboardSummary(
          currentSession,
          controller.signal,
        )

        setSummary(response)
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
            : 'The dashboard could not be loaded.',
        )
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false)
        }
      }
    }

    void loadDashboard()

    return () => {
      controller.abort()
    }
  }, [session, reloadVersion])

  if (!session) {
    return null
  }

  const { collaborator } = session


  function handleRetry() {
    setReloadVersion((current) => current + 1)
  }

  return (
    <main className="page">
      <section className="card card--wide">
        <AppHeader
          title="Dashboard"
          description={`Welcome, ${collaborator.name}.`}
        />

        <section
          className="account-panel"
          aria-labelledby="account-heading"
        >
          <h2 id="account-heading">
            Authenticated account
          </h2>

          <dl className="account-details">
            <div>
              <dt>Name</dt>
              <dd>{collaborator.name}</dd>
            </div>

            <div>
              <dt>Email</dt>
              <dd>{collaborator.email}</dd>
            </div>

            <div>
              <dt>Role</dt>
              <dd>{collaborator.role}</dd>
            </div>

            <div>
              <dt>Status</dt>
              <dd>
                {collaborator.active ? 'Active' : 'Inactive'}
              </dd>
            </div>
          </dl>
        </section>

        <div
          className="dashboard-content"
          aria-live="polite"
        >
          {isLoading && (
            <section className="dashboard-state">
              <h2>Loading dashboard</h2>
              <p>
                Retrieving projects, tasks and activity.
              </p>
            </section>
          )}

          {!isLoading && errorMessage && (
            <section
              className="dashboard-state dashboard-state--error"
              role="alert"
            >
              <h2>Dashboard unavailable</h2>
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

          {!isLoading && summary && (
            <>
              <section
                className="dashboard-section"
                aria-labelledby="overview-heading"
              >
                <div className="dashboard-section-heading">
                  <div>
                    <p className="eyebrow">Workspace</p>
                    <h2 id="overview-heading">Overview</h2>
                  </div>
                </div>

                <div className="metrics-grid">
                  <article className="metric-card">
                    <span>Collaborators</span>
                    <strong>{summary.collaboratorCount}</strong>
                  </article>

                  <article className="metric-card">
                    <span>Projects</span>
                    <strong>{summary.projectCount}</strong>
                  </article>

                  <article className="metric-card">
                    <span>Tasks</span>
                    <strong>{summary.taskCount}</strong>
                  </article>

                  <article className="metric-card">
                    <span>Programs</span>
                    <strong>{summary.programCount}</strong>
                    <small>Management UI planned</small>
                  </article>

                  <article className="metric-card">
                    <span>Tracked time</span>
                    <strong>
                      {formatTrackedTime(
                        summary.trackedTimeSeconds,
                      )}
                    </strong>
                  </article>
                </div>
              </section>

              <section
                className="dashboard-section"
                aria-labelledby="task-status-heading"
              >
                <div className="dashboard-section-heading">
                  <div>
                    <p className="eyebrow">Task workflow</p>
                    <h2 id="task-status-heading">
                      Tasks by status
                    </h2>
                  </div>
                </div>

                <div className="task-status-grid">
                  <article className="status-summary-card">
                    <span className="status-badge status-badge--pending">
                      Pending
                    </span>
                    <strong>
                      {summary.pendingTaskCount}
                    </strong>
                  </article>

                  <article className="status-summary-card">
                    <span className="status-badge status-badge--in-progress">
                      In progress
                    </span>
                    <strong>
                      {summary.inProgressTaskCount}
                    </strong>
                  </article>

                  <article className="status-summary-card">
                    <span className="status-badge status-badge--review">
                      Review
                    </span>
                    <strong>
                      {summary.reviewTaskCount}
                    </strong>
                  </article>

                  <article className="status-summary-card">
                    <span className="status-badge status-badge--completed">
                      Completed
                    </span>
                    <strong>
                      {summary.completedTaskCount}
                    </strong>
                  </article>
                </div>
              </section>

              <div className="dashboard-columns">
                <section
                  className="dashboard-data-panel"
                  aria-labelledby="recent-tasks-heading"
                >
                  <div className="dashboard-section-heading">
                    <div>
                      <p className="eyebrow">Latest activity</p>
                      <h2 id="recent-tasks-heading">
                        Recent tasks
                      </h2>
                    </div>
                  </div>

                  {summary.recentTasks.length === 0 ? (
                    <p className="dashboard-empty-state">
                      No tasks have been recorded yet.
                    </p>
                  ) : (
                    <ul className="dashboard-list">
                      {summary.recentTasks.map((task) => (
                        <RecentTask
                          key={task.id}
                          task={task}
                        />
                      ))}
                    </ul>
                  )}
                </section>

                <section
                  className="dashboard-data-panel"
                  aria-labelledby="upcoming-projects-heading"
                >
                  <div className="dashboard-section-heading">
                    <div>
                      <p className="eyebrow">Deadlines</p>
                      <h2 id="upcoming-projects-heading">
                        Upcoming projects
                      </h2>
                    </div>
                  </div>

                  {summary.upcomingProjects.length === 0 ? (
                    <p className="dashboard-empty-state">
                      No upcoming project deadlines.
                    </p>
                  ) : (
                    <ul className="dashboard-list">
                      {summary.upcomingProjects.map(
                        (project) => (
                          <UpcomingProject
                            key={project.id}
                            project={project}
                          />
                        ),
                      )}
                    </ul>
                  )}
                </section>
              </div>
            </>
          )}
        </div>
      </section>
    </main>
  )
}