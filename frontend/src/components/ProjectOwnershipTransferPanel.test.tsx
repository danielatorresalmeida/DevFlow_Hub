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
import { transferProjectOwnership } from '../api/projectOwnershipApi'
import type { AuthSession } from '../types/auth'
import type {
  ProjectMember,
  ProjectOwnershipTransferResponse,
} from '../types/projectMembership'
import { ProjectOwnershipTransferPanel } from './ProjectOwnershipTransferPanel'

vi.mock('../api/projectOwnershipApi', () => ({
  transferProjectOwnership: vi.fn(),
}))

const transferProjectOwnershipMock =
  vi.mocked(transferProjectOwnership)

function member(
  collaboratorId: number,
  name: string,
  role: ProjectMember['role'],
  version: number,
  status: ProjectMember['status'] = 'ACTIVE',
): ProjectMember {
  return {
    id: collaboratorId * 10,
    projectId: 11,
    collaboratorId,
    collaboratorName: name,
    collaboratorRole: 'Developer',
    role,
    status,
    version,
    createdAt: '2026-07-29T12:00:00',
    updatedAt: '2026-07-29T12:00:00',
  }
}

const members: ProjectMember[] = [
  member(7, 'Current Owner', 'OWNER', 2),
  member(8, 'Manager Member', 'MANAGER', 4),
  member(9, 'Contributor Member', 'CONTRIBUTOR', 1),
  member(10, 'Viewer Member', 'VIEWER', 3),
  member(11, 'Inactive Member', 'VIEWER', 0, 'INACTIVE'),
]

function session(
  collaboratorId: number,
): AuthSession {
  return {
    accessToken: 'test-token',
    tokenType: 'Bearer',
    expiresAt: Date.now() + 60_000,
    collaborator: {
      id: collaboratorId,
      name:
        collaboratorId === 7
          ? 'Current Owner'
          : 'Manager Member',
      email: 'member@example.com',
      role: 'Developer',
      active: true,
    },
  }
}

const response: ProjectOwnershipTransferResponse = {
  projectId: 11,
  managerId: 8,
  previousOwner: member(
    7,
    'Current Owner',
    'MANAGER',
    3,
  ),
  newOwner: member(
    8,
    'Manager Member',
    'OWNER',
    5,
  ),
}

function renderPanel(
  actorId = 7,
  overrides: Partial<{
    onTransferComplete: (
      value: ProjectOwnershipTransferResponse,
    ) => void
    onRefresh: () => void
  }> = {},
) {
  const onTransferComplete =
    overrides.onTransferComplete ?? vi.fn()
  const onRefresh = overrides.onRefresh ?? vi.fn()

  render(
    <ProjectOwnershipTransferPanel
      projectId={11}
      session={session(actorId)}
      members={members}
      onTransferComplete={onTransferComplete}
      onRefresh={onRefresh}
    />,
  )

  return {
    onTransferComplete,
    onRefresh,
  }
}

describe('ProjectOwnershipTransferPanel', () => {
  beforeEach(() => {
    transferProjectOwnershipMock.mockReset()
    vi.restoreAllMocks()
  })

  it('is available only to the active project owner', () => {
    renderPanel(8)

    expect(
      screen.queryByRole('heading', {
        name: 'Transfer ownership',
      }),
    ).toBeNull()
  })

  it('lists every active non-owner member as eligible', () => {
    renderPanel()

    const select = screen.getByLabelText('New owner')
    const options = within(select)
      .getAllByRole('option')
      .map((option) => option.textContent)

    expect(options).toEqual([
      'Select a project member',
      'Contributor Member (Contributor)',
      'Manager Member (Manager)',
      'Viewer Member (Viewer)',
    ])
  })

  it('requires both confirmation steps before sending the request', () => {
    renderPanel()

    const button = screen.getByRole('button', {
      name: 'Transfer ownership',
    })

    expect(
      (button as HTMLButtonElement).disabled,
    ).toBe(true)

    fireEvent.change(
      screen.getByLabelText('New owner'),
      {
        target: { value: '8' },
      },
    )

    fireEvent.click(
      screen.getByRole('checkbox'),
    )

    expect(
      (button as HTMLButtonElement).disabled,
    ).toBe(false)

    vi.spyOn(window, 'confirm')
      .mockReturnValue(false)

    fireEvent.click(button)

    expect(
      transferProjectOwnershipMock,
    ).not.toHaveBeenCalled()
  })

  it('sends both membership versions and reports the transfer', async () => {
    transferProjectOwnershipMock
      .mockResolvedValue(response)

    const confirmSpy = vi.spyOn(window, 'confirm')
      .mockReturnValue(true)

    const { onTransferComplete } = renderPanel()

    fireEvent.change(
      screen.getByLabelText('New owner'),
      {
        target: { value: '8' },
      },
    )

    fireEvent.click(
      screen.getByRole('checkbox'),
    )

    fireEvent.click(
      screen.getByRole('button', {
        name: 'Transfer ownership',
      }),
    )

    await waitFor(() => {
      expect(
        transferProjectOwnershipMock,
      ).toHaveBeenCalledWith(
        11,
        {
          newOwnerCollaboratorId: 8,
          currentOwnerMembershipVersion: 2,
          newOwnerMembershipVersion: 4,
        },
        expect.objectContaining({
          accessToken: 'test-token',
        }),
      )
    })

    expect(confirmSpy).toHaveBeenCalledWith(
      expect.stringContaining('Manager Member'),
    )
    expect(onTransferComplete)
      .toHaveBeenCalledWith(response)
  })

  it('shows conflict feedback without duplicating the backend message', async () => {
    transferProjectOwnershipMock.mockRejectedValue(
      new ApiClientError(
        409,
        'New owner membership was modified by another request. Refresh and try again.',
      ),
    )

    vi.spyOn(window, 'confirm')
      .mockReturnValue(true)

    const { onRefresh } = renderPanel()

    fireEvent.change(
      screen.getByLabelText('New owner'),
      {
        target: { value: '8' },
      },
    )

    fireEvent.click(
      screen.getByRole('checkbox'),
    )

    fireEvent.click(
      screen.getByRole('button', {
        name: 'Transfer ownership',
      }),
    )

    expect(
      await screen.findByText(
        'New owner membership was modified by another request. Refresh and try again.',
      ),
    ).toBeTruthy()

    expect(
      screen.queryByText(
        /Refresh and try again\. Refresh/,
      ),
    ).toBeNull()

    fireEvent.click(
      screen.getByRole('button', {
        name: 'Refresh project data',
      }),
    )

    expect(onRefresh).toHaveBeenCalledTimes(1)
  })
})
