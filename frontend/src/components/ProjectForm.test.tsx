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
import { ApiClientError } from '../api/apiClient'
import {
  createProject,
  updateProject,
} from '../api/projectsApi'
import type { AuthSession } from '../types/auth'
import type { Project } from '../types/project'
import { ProjectForm } from './ProjectForm'

vi.mock('../api/projectsApi', () => ({
  createProject: vi.fn(),
  updateProject: vi.fn(),
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

const savedProject: Project = {
  id: 7,
  name: 'Platform Project',
  description: null,
  status: 'PLANNED',
  startDate: null,
  endDate: null,
  managerId: 1,
}

describe('ProjectForm', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(createProject)
      .mockResolvedValue(savedProject)
    vi.mocked(updateProject)
      .mockResolvedValue(savedProject)
  })

  it('creates a project owned and managed by the authenticated collaborator', async () => {
    const onSaved = vi.fn()

    render(
      <ProjectForm
        mode="create"
        session={session}
        onSaved={onSaved}
        onCancel={vi.fn()}
      />,
    )

    fireEvent.change(
      screen.getByLabelText('Name'),
      {
        target: { value: 'Platform Project' },
      },
    )

    fireEvent.click(
      screen.getByRole('button', {
        name: 'Create project',
      }),
    )

    await waitFor(() => {
      expect(createProject).toHaveBeenCalledWith(
        {
          name: 'Platform Project',
          description: null,
          status: 'PLANNED',
          startDate: null,
          endDate: null,
          managerId: 1,
        },
        session,
      )
    })

    expect(onSaved).toHaveBeenCalledWith(savedProject)
  })

  it('updates project fields and the selected manager', async () => {
    render(
      <ProjectForm
        mode="edit"
        session={session}
        initialProject={savedProject}
        managerOptions={[
          { id: 1, name: 'Owner User' },
          { id: 2, name: 'Manager User' },
        ]}
        onSaved={vi.fn()}
        onCancel={vi.fn()}
      />,
    )

    fireEvent.change(
      screen.getByLabelText('Name'),
      {
        target: { value: 'Updated Project' },
      },
    )

    fireEvent.change(
      screen.getByLabelText('Status'),
      {
        target: { value: 'IN_PROGRESS' },
      },
    )

    fireEvent.change(
      screen.getByLabelText('Manager'),
      {
        target: { value: '2' },
      },
    )

    fireEvent.click(
      screen.getByRole('button', {
        name: 'Save project',
      }),
    )

    await waitFor(() => {
      expect(updateProject).toHaveBeenCalledWith(
        7,
        expect.objectContaining({
          name: 'Updated Project',
          status: 'IN_PROGRESS',
          managerId: 2,
        }),
        session,
      )
    })
  })

  it('rejects an end date before the start date', async () => {
    render(
      <ProjectForm
        mode="create"
        session={session}
        onSaved={vi.fn()}
        onCancel={vi.fn()}
      />,
    )

    fireEvent.change(
      screen.getByLabelText('Name'),
      {
        target: { value: 'Invalid dates' },
      },
    )

    fireEvent.change(
      screen.getByLabelText('Start date'),
      {
        target: { value: '2026-08-10' },
      },
    )

    fireEvent.change(
      screen.getByLabelText('End date'),
      {
        target: { value: '2026-08-01' },
      },
    )

    fireEvent.click(
      screen.getByRole('button', {
        name: 'Create project',
      }),
    )

    expect(
      await screen.findByRole('alert'),
    ).toHaveTextContent(
      'Project end date cannot be before its start date.',
    )
    expect(createProject).not.toHaveBeenCalled()
  })

  it('shows a validation message returned by the API', async () => {
    vi.mocked(createProject).mockRejectedValue(
      new ApiClientError(
        400,
        'Validation failed.',
        {
          name: 'Name is required.',
        },
      ),
    )

    render(
      <ProjectForm
        mode="create"
        session={session}
        onSaved={vi.fn()}
        onCancel={vi.fn()}
      />,
    )

    fireEvent.change(
      screen.getByLabelText('Name'),
      {
        target: { value: 'Project' },
      },
    )

    fireEvent.click(
      screen.getByRole('button', {
        name: 'Create project',
      }),
    )

    expect(
      await screen.findByRole('alert'),
    ).toHaveTextContent('Name is required.')
  })
})
