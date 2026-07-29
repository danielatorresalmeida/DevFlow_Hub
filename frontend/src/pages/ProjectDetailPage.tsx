import { useEffect, useState } from 'react'
import {
  Link,
  useNavigate,
  useParams,
} from 'react-router'
import { ApiClientError } from '../api/apiClient'
import { getCollaborators } from '../api/collaboratorsApi'
import { getProjectMembers } from '../api/projectMembershipsApi'
import {
  deleteProject,
  getProjectById,
} from '../api/projectsApi'
import { getTasks } from '../api/tasksApi'
import { useAuth } from '../auth/useAuth'
import { AppHeader } from '../components/AppHeader'
import { ProjectForm } from '../components/ProjectForm'
import type { ProjectManagerOption } from '../components/ProjectForm'
import { ProjectMembersPanel } from '../components/ProjectMembersPanel'
import { ProjectOwnershipTransferPanel } from '../components/ProjectOwnershipTransferPanel'
import type { Collaborator } from '../types/collaborator'
import type { Project } from '../types/project'
import type {
  ProjectMember,
  ProjectOwnershipTransferResponse,
} from '../types/projectMembership'
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
  const navigate = useNavigate()
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

  const [collaborators, setCollaborators] =
    useState<Collaborator[]>([])

  const [projectMembers, setProjectMembers] =
    useState<ProjectMember[]>([])

  const [ownershipTransferNotice, setOwnershipTransferNotice] =
    useState<string | null>(null)

  const [isEditing, setIsEditing] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const [projectActionMessage, setProjectActionMessage] =
    useState<string | null>(null)
  const [projectActionError, setProjectActionError] =
    useState<string | null>(null)

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
      setOwnershipTransferNotice(null)
      setProjectActionMessage(null)
      setProjectActionError(null)

      try {
        const [
          projectResponse,
          collaboratorResponse,
          taskResponse,
          memberResponse,
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
          getProjectMembers(
            projectId,
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
        setCollaborators(collaboratorResponse)
        setProjectMembers(memberResponse)
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
          setCollaborators([])
          setProjectMembers([])
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

  const currentMembership = projectMembers.find(
    (member) => (
      member.collaboratorId === session.collaborator.id &&
      member.status === 'ACTIVE'
    ),
  )

  const canEditProject =
    currentMembership?.role === 'OWNER' ||
    currentMembership?.role === 'MANAGER'

  const canDeleteProject =
    currentMembership?.role === 'OWNER'

  const managerOptions: ProjectManagerOption[] =
    projectMembers
      .filter((member) => (
        member.status === 'ACTIVE' &&
        (
          member.role === 'OWNER' ||
          member.role === 'MANAGER'
        )
      ))
      .map((member) => ({
        id: member.collaboratorId,
        name: member.collaboratorName,
      }))

  function handleProjectSaved(savedProject: Project) {
    setProject(savedProject)
    setManagerName(
      managerOptions.find(
        (manager) => manager.id === savedProject.managerId,
      )?.name ?? 'Not assigned',
    )
    setIsEditing(false)
    setProjectActionError(null)
    setProjectActionMessage(
      `Project #${savedProject.id} was updated.`,
    )
  }

  async function handleDeleteProject() {
    if (!session || !project || isDeleting) {
      return
    }

    const confirmed = window.confirm(
      `Delete project "${project.name}"? Associated tasks will remain available without this project.`,
    )

    if (!confirmed) {
      return
    }

    setIsDeleting(true)
    setProjectActionMessage(null)
    setProjectActionError(null)

    try {
      await deleteProject(project.id, session)
      navigate('/projects', { replace: true })
    } catch (error) {
      if (
        error instanceof ApiClientError &&
        error.status === 401
      ) {
        return
      }

      setProjectActionError(
        error instanceof Error
          ? error.message
          : 'The project could not be deleted.',
      )
    } finally {
      setIsDeleting(false)
    }
  }

  function handleOwnershipTransferred(
    response: ProjectOwnershipTransferResponse,
  ) {
    setProject((currentProject) => (
      currentProject
        ? {
            ...currentProject,
            managerId: response.managerId,
          }
        : currentProject
    ))

    setManagerName(
      response.newOwner.collaboratorName,
    )

    setProjectMembers((currentMembers) => (
      currentMembers.map((member) => {
        if (
          member.collaboratorId ===
          response.previousOwner.collaboratorId
        ) {
          return response.previousOwner
        }

        if (
          member.collaboratorId ===
          response.newOwner.collaboratorId
        ) {
          return response.newOwner
        }

        return member
      })
    ))

    setOwnershipTransferNotice(
      `Ownership was transferred to ${response.newOwner.collaboratorName}. Your project role is now Manager.`,
    )
  }

  return (
    <main className="page">
      <section className="card card--wide">
        <AppHeader
          title={project?.name ?? 'Project detail'}
          description="Review project information, members and associated tasks."
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
                Retrieving the project, members, manager and tasks.
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

                {(canEditProject || canDeleteProject) && (
                  <section className="task-management-toolbar">
                    <div>
                      <p className="eyebrow">
                        Project management
                      </p>
                      <p>
                        Update the project information or remove
                        it when it is no longer required.
                      </p>
                    </div>

                    <div className="task-detail-actions">
                      {canEditProject && (
                        <button
                          className="task-action-button"
                          type="button"
                          onClick={() => {
                            setIsEditing((current) => !current)
                            setProjectActionMessage(null)
                            setProjectActionError(null)
                          }}
                          disabled={isDeleting}
                        >
                          {isEditing
                            ? 'Close edit form'
                            : 'Edit project'}
                        </button>
                      )}

                      {canDeleteProject && (
                        <button
                          className="task-action-button task-action-button--danger"
                          type="button"
                          onClick={() => {
                            void handleDeleteProject()
                          }}
                          disabled={isDeleting}
                        >
                          {isDeleting
                            ? 'Deleting...'
                            : 'Delete project'}
                        </button>
                      )}
                    </div>
                  </section>
                )}

                {isEditing && canEditProject && (
                  <ProjectForm
                    mode="edit"
                    session={session}
                    initialProject={project}
                    managerOptions={managerOptions}
                    onSaved={handleProjectSaved}
                    onCancel={() => {
                      setIsEditing(false)
                    }}
                  />
                )}

                {projectActionMessage && (
                  <p
                    className="task-action-message task-action-message--success"
                    role="status"
                  >
                    {projectActionMessage}
                  </p>
                )}

                {projectActionError && (
                  <p
                    className="task-action-message task-action-message--error"
                    role="alert"
                  >
                    {projectActionError}
                  </p>
                )}

                {ownershipTransferNotice && (
                  <p
                    className="project-member-message project-member-message--success"
                    role="status"
                  >
                    {ownershipTransferNotice}
                  </p>
                )}

                <ProjectOwnershipTransferPanel
                  projectId={project.id}
                  session={session}
                  members={projectMembers}
                  onTransferComplete={
                    handleOwnershipTransferred
                  }
                  onRefresh={handleRetry}
                />

                <ProjectMembersPanel
                  projectId={project.id}
                  projectManagerId={project.managerId}
                  session={session}
                  collaborators={collaborators}
                  members={projectMembers}
                  onMembersChange={setProjectMembers}
                />

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

                          <footer className="project-task-footer">
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
                  )}
                </section>
              </>
            )}
        </div>
      </section>
    </main>
  )
}