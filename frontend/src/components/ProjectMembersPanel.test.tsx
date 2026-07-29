import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
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
  updateProjectMemberRole,
} from '../api/projectMembershipsApi'
import type { AuthSession } from '../types/auth'
import type { Collaborator } from '../types/collaborator'
import type { ProjectMember } from '../types/projectMembership'
import { ProjectMembersPanel } from './ProjectMembersPanel'

vi.mock('../api/projectMembershipsApi', () => ({
  addProjectMember: vi.fn(),
  removeProjectMember: vi.fn(),
  updateProjectMemberRole: vi.fn(),
}))

const collaborators: Collaborator[] = [
  {
    id: 1,
    name: 'Owner User',
    email: 'owner@example.com',
    role: 'Developer',
    active: true,
  },
  {
    id: 2,
    name: 'Manager Member',
    email: 'manager@example.com',
    role: 'Project Manager',
    active: true,
  },
  {
    id: 3,
    name: 'Contributor Member',
    email: 'contributor@example.com',
    role: 'Developer',
    active: true,
  },
  {
    id: 4,
    name: 'Available Collaborator',
    email: 'available@example.com',
    role: 'Designer',
    active: true,
  },
]

function createSession(
  collaboratorId: number,
): AuthSession {
  const collaborator = collaborators.find(
    (item) => item.id === collaboratorId,
  )

  if (!collaborator) {
    throw new Error('Test collaborator not found.')
  }

  return {
    accessToken: 'test-token',
    tokenType: 'Bearer',
    expiresAt: Date.now() + 60_000,
    collaborator,
  }
}

function createMember(
  collaboratorId: number,
  role: ProjectMember['role'],
): ProjectMember {
  const collaborator = collaborators.find(
    (item) => item.id === collaboratorId,
  )

  if (!collaborator) {
    throw new Error('Test collaborator not found.')
  }

  return {
    id: collaboratorId * 10,
    projectId: 7,
    collaboratorId,
    collaboratorName: collaborator.name,
    collaboratorRole: collaborator.role,
    role,
    status: 'ACTIVE',
    version: 0,
    createdAt: '2026-07-29T14:00:00',
    updatedAt: '2026-07-29T14:00:00',
  }
}

describe('ProjectMembersPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shows read-only access to contributors', () => {
    render(
      <ProjectMembersPanel
        projectId={7}
        projectManagerId={2}
        session={createSession(3)}
        collaborators={collaborators}
        members={[
          createMember(1, 'OWNER'),
          createMember(2, 'MANAGER'),
          createMember(3, 'CONTRIBUTOR'),
        ]}
        onMembersChange={vi.fn()}
      />,
    )

    expect(
      screen.getByText('Read-only access'),
    ).toBeTruthy()

    expect(
      screen.queryByRole('button', {
        name: 'Add member',
      }),
    ).toBeNull()
  })

  it('allows owners to assign every managed role but not owner', () => {
    render(
      <ProjectMembersPanel
        projectId={7}
        projectManagerId={1}
        session={createSession(1)}
        collaborators={collaborators}
        members={[
          createMember(1, 'OWNER'),
          createMember(2, 'MANAGER'),
          createMember(3, 'CONTRIBUTOR'),
        ]}
        onMembersChange={vi.fn()}
      />,
    )

    const roleSelect = screen.getByLabelText(
      'Membership role',
    )

    const options = within(roleSelect)
      .getAllByRole('option')
      .map((option) => option.textContent)

    expect(options).toEqual([
      'Manager',
      'Contributor',
      'Viewer',
    ])
  })

  it('prevents managers from managing manager memberships', () => {
    render(
      <ProjectMembersPanel
        projectId={7}
        projectManagerId={2}
        session={createSession(2)}
        collaborators={collaborators}
        members={[
          createMember(1, 'OWNER'),
          createMember(2, 'MANAGER'),
          createMember(3, 'CONTRIBUTOR'),
        ]}
        onMembersChange={vi.fn()}
      />,
    )

    const managerCard = screen
      .getByRole('heading', {
        name: 'Manager Member',
      })
      .closest('article')

    const contributorCard = screen
      .getByRole('heading', {
        name: 'Contributor Member',
      })
      .closest('article')

    expect(managerCard).not.toBeNull()
    expect(contributorCard).not.toBeNull()

    expect(
      within(managerCard as HTMLElement)
        .queryByRole('combobox'),
    ).toBeNull()

    expect(
      within(contributorCard as HTMLElement)
        .getByRole('combobox'),
    ).toBeTruthy()
  })

  it('shows a conflict message without repeating refresh instructions', async () => {
    vi.mocked(updateProjectMemberRole)
      .mockRejectedValue(
        new ApiClientError(
          409,
          'Refresh and try again.',
        ),
      )

    render(
      <ProjectMembersPanel
        projectId={7}
        projectManagerId={1}
        session={createSession(1)}
        collaborators={collaborators}
        members={[
          createMember(1, 'OWNER'),
          createMember(2, 'MANAGER'),
          createMember(3, 'CONTRIBUTOR'),
        ]}
        onMembersChange={vi.fn()}
      />,
    )

    const contributorCard = screen
      .getByRole('heading', {
        name: 'Contributor Member',
      })
      .closest('article')

    expect(contributorCard).not.toBeNull()

    fireEvent.change(
      within(contributorCard as HTMLElement)
        .getByRole('combobox'),
      {
        target: {
          value: 'VIEWER',
        },
      },
    )

    fireEvent.click(
      within(contributorCard as HTMLElement)
        .getByRole('button', {
          name: 'Save role',
        }),
    )

    await waitFor(() => {
      expect(
        screen.getByRole('alert').textContent,
      ).toBe('Refresh and try again.')
    })
  })

})
