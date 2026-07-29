import {
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/react'
import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'
import { getProjectMembers } from '../api/projectMembershipsApi'
import {
  createTask,
  updateTask,
} from '../api/tasksApi'
import type { AuthSession } from '../types/auth'
import type { Project } from '../types/project'
import type { ProjectMember } from '../types/projectMembership'
import type { Task } from '../types/task'
import { TaskForm } from './TaskForm'

vi.mock('../api/projectMembershipsApi', () => ({
  getProjectMembers: vi.fn(),
}))

vi.mock('../api/tasksApi', () => ({
  createTask: vi.fn(),
  updateTask: vi.fn(),
}))

const session: AuthSession = {
  accessToken: 'test-token',
  tokenType: 'Bearer',
  expiresAt: Date.now() + 60_000,
  collaborator: {
    id: 1,
    name: 'Owner User',
    email: 'owner@example.com',
    role: 'Developer',
    active: true,
  },
}

const projects: Project[] = [
  {
    id: 7,
    name: 'Platform Project',
    description: null,
    status: 'ACTIVE',
    startDate: null,
    endDate: null,
    managerId: 1,
  },
]

const members: ProjectMember[] = [
  {
    id: 10,
    projectId: 7,
    collaboratorId: 1,
    collaboratorName: 'Owner User',
    collaboratorRole: 'Developer',
    role: 'OWNER',
    status: 'ACTIVE',
    version: 0,
    createdAt: '2026-07-29T18:00:00',
    updatedAt: '2026-07-29T18:00:00',
  },
  {
    id: 20,
    projectId: 7,
    collaboratorId: 2,
    collaboratorName: 'Contributor User',
    collaboratorRole: 'Developer',
    role: 'CONTRIBUTOR',
    status: 'ACTIVE',
    version: 0,
    createdAt: '2026-07-29T18:00:00',
    updatedAt: '2026-07-29T18:00:00',
  },
]

const savedTask: Task = {
  id: 12,
  title: 'Prepare release',
  description: null,
  status: 'PENDING',
  priority: 'MEDIUM',
  projectId: null,
  assigneeId: 1,
  totalTimeSeconds: 0,
  timerActive: false,
  timerStartedAt: null,
  createdAt: '2026-07-29T18:00:00',
  updatedAt: '2026-07-29T18:00:00',
}

describe('TaskForm', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(getProjectMembers)
      .mockResolvedValue(members)
    vi.mocked(createTask)
      .mockResolvedValue(savedTask)
    vi.mocked(updateTask)
      .mockResolvedValue(savedTask)
  })

  it('creates a standalone task assigned to the authenticated collaborator', async () => {
    const onSaved = vi.fn()

    render(
      <TaskForm
        mode="create"
        session={session}
        projects={projects}
        onSaved={onSaved}
        onCancel={vi.fn()}
      />,
    )

    fireEvent.change(
      screen.getByLabelText('Title'),
      {
        target: { value: 'Prepare release' },
      },
    )

    fireEvent.click(
      screen.getByRole('button', {
        name: 'Create task',
      }),
    )

    await waitFor(() => {
      expect(createTask).toHaveBeenCalledWith(
        {
          title: 'Prepare release',
          description: null,
          status: 'PENDING',
          priority: 'MEDIUM',
          projectId: null,
          assigneeId: 1,
        },
        session,
      )
    })

    expect(onSaved).toHaveBeenCalledWith(savedTask)
  })

  it('loads project members and assigns a project task', async () => {
    render(
      <TaskForm
        mode="create"
        session={session}
        projects={projects}
        onSaved={vi.fn()}
        onCancel={vi.fn()}
      />,
    )

    fireEvent.change(
      screen.getByLabelText('Project'),
      {
        target: { value: '7' },
      },
    )

    await waitFor(() => {
      expect(getProjectMembers).toHaveBeenCalledWith(
        7,
        session,
        expect.any(AbortSignal),
      )
    })

    fireEvent.change(
      screen.getByLabelText('Assignee'),
      {
        target: { value: '2' },
      },
    )

    fireEvent.change(
      screen.getByLabelText('Title'),
      {
        target: { value: 'Review API' },
      },
    )

    fireEvent.click(
      screen.getByRole('button', {
        name: 'Create task',
      }),
    )

    await waitFor(() => {
      expect(createTask).toHaveBeenCalledWith(
        expect.objectContaining({
          title: 'Review API',
          projectId: 7,
          assigneeId: 2,
        }),
        session,
      )
    })
  })

  it('updates the existing task fields', async () => {
    const initialTask: Task = {
      ...savedTask,
      title: 'Initial title',
      status: 'IN_PROGRESS',
      priority: 'LOW',
    }

    render(
      <TaskForm
        mode="edit"
        session={session}
        projects={projects}
        initialTask={initialTask}
        onSaved={vi.fn()}
        onCancel={vi.fn()}
      />,
    )

    fireEvent.change(
      screen.getByLabelText('Title'),
      {
        target: { value: 'Updated title' },
      },
    )

    fireEvent.change(
      screen.getByLabelText('Status'),
      {
        target: { value: 'REVIEW' },
      },
    )

    fireEvent.click(
      screen.getByRole('button', {
        name: 'Save task',
      }),
    )

    await waitFor(() => {
      expect(updateTask).toHaveBeenCalledWith(
        12,
        expect.objectContaining({
          title: 'Updated title',
          status: 'REVIEW',
        }),
        session,
      )
    })
  })

  it('prevents viewers from creating project tasks', async () => {
    vi.mocked(getProjectMembers).mockResolvedValue([
      {
        ...members[0],
        role: 'VIEWER',
      },
    ])

    render(
      <TaskForm
        mode="create"
        session={session}
        projects={projects}
        onSaved={vi.fn()}
        onCancel={vi.fn()}
      />,
    )

    fireEvent.change(
      screen.getByLabelText('Project'),
      {
        target: { value: '7' },
      },
    )

    await waitFor(() => {
      expect(
        screen.getByText(
          'Your Viewer membership allows consultation only.',
        ),
      ).toBeTruthy()
    })

    expect(
      screen.getByRole('button', {
        name: 'Create task',
      }),
    ).toBeDisabled()
  })
})
