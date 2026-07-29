import {
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'
import type { AuthSession } from '../types/auth'
import type { ProjectMember } from '../types/projectMembership'
import {
  addProjectMember,
  getProjectMembers,
  removeProjectMember,
  updateProjectMemberRole,
} from './projectMembershipsApi'

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

const member: ProjectMember = {
  id: 10,
  projectId: 7,
  collaboratorId: 3,
  collaboratorName: 'New Member',
  collaboratorRole: 'Developer',
  role: 'CONTRIBUTOR',
  status: 'ACTIVE',
  version: 0,
  createdAt: '2026-07-29T14:00:00',
  updatedAt: '2026-07-29T14:00:00',
}

const fetchMock = vi.fn()

function jsonResponse(
  body: unknown,
  status = 200,
): Response {
  return new Response(
    JSON.stringify(body),
    {
      status,
      headers: {
        'Content-Type': 'application/json',
      },
    },
  )
}

describe('projectMembershipsApi', () => {
  beforeEach(() => {
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('loads active project members', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse([member]),
    )

    await expect(
      getProjectMembers(7, session),
    ).resolves.toEqual([member])

    const [url, options] = fetchMock.mock.calls[0]

    expect(url).toBe('/api/projects/7/members')
    expect(options.method).toBe('GET')
    expect(
      new Headers(options.headers).get('Authorization'),
    ).toBe('Bearer test-token')
  })

  it('adds a project member with the selected role', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(member, 201),
    )

    await addProjectMember(
      7,
      {
        collaboratorId: 3,
        role: 'CONTRIBUTOR',
      },
      session,
    )

    const [url, options] = fetchMock.mock.calls[0]

    expect(url).toBe('/api/projects/7/members')
    expect(options.method).toBe('POST')
    expect(JSON.parse(options.body as string)).toEqual({
      collaboratorId: 3,
      role: 'CONTRIBUTOR',
    })
  })

  it('sends the current membership version when changing a role', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse({
        ...member,
        role: 'VIEWER',
        version: 1,
      }),
    )

    await updateProjectMemberRole(
      7,
      3,
      {
        role: 'VIEWER',
        version: 0,
      },
      session,
    )

    const [url, options] = fetchMock.mock.calls[0]

    expect(url).toBe('/api/projects/7/members/3')
    expect(options.method).toBe('PATCH')
    expect(JSON.parse(options.body as string)).toEqual({
      role: 'VIEWER',
      version: 0,
    })
  })

  it('removes a project member through the dedicated endpoint', async () => {
    fetchMock.mockResolvedValue(
      new Response(null, { status: 204 }),
    )

    await expect(
      removeProjectMember(7, 3, session),
    ).resolves.toBeUndefined()

    const [url, options] = fetchMock.mock.calls[0]

    expect(url).toBe('/api/projects/7/members/3')
    expect(options.method).toBe('DELETE')
  })
})
