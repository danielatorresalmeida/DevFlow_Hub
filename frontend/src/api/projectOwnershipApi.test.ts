import {
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'
import type { AuthSession } from '../types/auth'
import type {
  ProjectMember,
  ProjectOwnershipTransferResponse,
} from '../types/projectMembership'
import { transferProjectOwnership } from './projectOwnershipApi'

const session: AuthSession = {
  accessToken: 'test-token',
  tokenType: 'Bearer',
  expiresAt: Date.now() + 60_000,
  collaborator: {
    id: 7,
    name: 'Current Owner',
    email: 'owner@example.com',
    role: 'Developer',
    active: true,
  },
}

function member(
  collaboratorId: number,
  role: ProjectMember['role'],
  version: number,
): ProjectMember {
  return {
    id: collaboratorId * 10,
    projectId: 11,
    collaboratorId,
    collaboratorName:
      collaboratorId === 7
        ? 'Current Owner'
        : 'New Owner',
    collaboratorRole: 'Developer',
    role,
    status: 'ACTIVE',
    version,
    createdAt: '2026-07-29T12:00:00',
    updatedAt: '2026-07-29T12:00:00',
  }
}

const response: ProjectOwnershipTransferResponse = {
  projectId: 11,
  managerId: 8,
  previousOwner: member(7, 'MANAGER', 3),
  newOwner: member(8, 'OWNER', 5),
}

const fetchMock = vi.fn()

function jsonResponse(body: unknown): Response {
  return new Response(
    JSON.stringify(body),
    {
      status: 200,
      headers: {
        'Content-Type': 'application/json',
      },
    },
  )
}

describe('projectOwnershipApi', () => {
  beforeEach(() => {
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('transfers ownership with both current membership versions', async () => {
    fetchMock.mockResolvedValue(jsonResponse(response))

    await expect(
      transferProjectOwnership(
        11,
        {
          newOwnerCollaboratorId: 8,
          currentOwnerMembershipVersion: 2,
          newOwnerMembershipVersion: 4,
        },
        session,
      ),
    ).resolves.toEqual(response)

    const [url, options] = fetchMock.mock.calls[0]

    expect(url).toBe(
      '/api/projects/11/ownership-transfer',
    )
    expect(options.method).toBe('POST')
    expect(JSON.parse(options.body as string)).toEqual({
      newOwnerCollaboratorId: 8,
      currentOwnerMembershipVersion: 2,
      newOwnerMembershipVersion: 4,
    })
    expect(
      new Headers(options.headers).get('Authorization'),
    ).toBe('Bearer test-token')
  })
})
