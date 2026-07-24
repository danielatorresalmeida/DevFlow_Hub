import { useEffect, useState } from 'react'
import { ApiClientError } from '../api/apiClient'
import { getCollaborators } from '../api/collaboratorsApi'
import { getProjects } from '../api/projectsApi'
import { useAuth } from '../auth/useAuth'
import { AppHeader } from '../components/AppHeader'
import type { ProjectListItem } from '../types/project'

const dateFormatter = new Intl.DateTimeFormat(undefined, {
  dateStyle: 'medium',
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

function formatDate(value: string | null): string {
  if (!value) {
    return 'Not set'
  }

  return dateFormatter.format(
    new Date(`${value}T00:00:00`),
  )
}

export function ProjectsPage() {
  const { session } = useAuth()

  const [projects, setProjects] =
    useState<ProjectListItem[]>([])

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

    async function loadProjects() {
      setIsLoading(true)
      setErrorMessage(null)

      try {
        const [
          projectResponse,
          collaboratorResponse,
        ] = await Promise.all([
          getProjects(
            currentSession,
            controller.signal,
          ),
          getCollaborators(
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

        const projectItems = projectResponse.map(
          (project): ProjectListItem => ({
            ...project,
            managerName:
              project.managerId === null
                ? 'Not assigned'
                : collaboratorNames.get(
                    project.managerId,
                  ) ?? 'Not assigned',
          }),
        )

        setProjects(projectItems)
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
            : 'The projects could not be loaded.',
        )
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false)
        }
      }
    }

    void loadProjects()

    return () => {
      controller.abort()
    }
  }, [session, reloadVersion])

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
          title="Projects"
          description="Review the projects currently registered in the workspace."
        />

        <div
          className="projects-content"
          aria-live="polite"
        >
          {isLoading && (
            <section className="dashboard-state">
              <h2>Loading projects</h2>
              <p>
                Retrieving project and manager information.
              </p>
            </section>
          )}

          {!isLoading && errorMessage && (
            <section
              className="dashboard-state dashboard-state--error"
              role="alert"
            >
              <h2>Projects unavailable</h2>
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
            projects.length === 0 && (
              <section className="dashboard-state">
                <h2>No projects found</h2>
                <p>
                  Projects created in the workspace will
                  appear here.
                </p>
              </section>
            )}

          {!isLoading &&
            !errorMessage &&
            projects.length > 0 && (
              <>
                <div className="projects-summary">
                  <p>
                    <strong>{projects.length}</strong>{' '}
                    {projects.length === 1
                      ? 'project'
                      : 'projects'}
                  </p>
                </div>

                <div className="projects-grid">
                  {projects.map((project) => (
                    <article
                      className="project-card"
                      key={project.id}
                    >
                      <header className="project-card-header">
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
                          {formatStatus(project.status)}
                        </span>
                      </header>

                      <p className="project-description">
                        {project.description?.trim() ||
                          'No description has been added.'}
                      </p>

                      <dl className="project-details">
                        <div>
                          <dt>Manager</dt>
                          <dd>{project.managerName}</dd>
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
                      </dl>

                      <footer className="project-card-footer">
                        <span>
                          Tasks and documentation will be
                          available in the project detail page.
                        </span>
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