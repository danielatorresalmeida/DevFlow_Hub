import {
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'
import type { AuthSession } from '../types/auth'
import type { Project, ProjectInput } from '../types/project'
import {
  createProject,
  deleteProject,
  updateProject,
} from './projectsApi'

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

const input: ProjectInput = {
  name: 'Platform Project',
  description: 'Build the shared platform.',
  status: 'IN_PROGRESS',
  startDate: '2026-07-01',
  endDate: '2026-08-31',
  managerId: 1,
}

const project: Project = {
  id: 7,
  ...input,
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

describe('projectsApi management operations', () => {
  beforeEach(() => {
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('creates a project with the selected fields', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(project, 201),
    )

    await expect(
      createProject(input, session),
    ).resolves.toEqual(project)

    const [url, options] = fetchMock.mock.calls[0]

    expect(url).toBe('/api/projects')
    expect(options.method).toBe('POST')
    expect(JSON.parse(options.body as string)).toEqual(input)
  })

  it('updates a project through the resource endpoint', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse({
        ...project,
        status: 'COMPLETED',
      }),
    )

    await updateProject(
      7,
      {
        ...input,
        status: 'COMPLETED',
      },
      session,
    )

    const [url, options] = fetchMock.mock.calls[0]

    expect(url).toBe('/api/projects/7')
    expect(options.method).toBe('PUT')
    expect(JSON.parse(options.body as string)).toEqual({
      ...input,
      status: 'COMPLETED',
    })
  })

  it('deletes a project through the resource endpoint', async () => {
    fetchMock.mockResolvedValue(
      new Response(null, { status: 204 }),
    )

    await expect(
      deleteProject(7, session),
    ).resolves.toBeUndefined()

    const [url, options] = fetchMock.mock.calls[0]

    expect(url).toBe('/api/projects/7')
    expect(options.method).toBe('DELETE')
  })
})
