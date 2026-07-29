import {
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'
import type { AuthSession } from '../types/auth'
import type { Task, TaskInput } from '../types/task'
import {
  createTask,
  deleteTask,
  updateTask,
} from './tasksApi'

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

const input: TaskInput = {
  title: 'Prepare release',
  description: 'Validate the final build.',
  status: 'PENDING',
  priority: 'HIGH',
  projectId: 7,
  assigneeId: 3,
}

const task: Task = {
  id: 12,
  ...input,
  totalTimeSeconds: 0,
  timerActive: false,
  timerStartedAt: null,
  createdAt: '2026-07-29T18:00:00',
  updatedAt: '2026-07-29T18:00:00',
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

describe('tasksApi management operations', () => {
  beforeEach(() => {
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('creates a task with the selected fields', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(task, 201),
    )

    await expect(
      createTask(input, session),
    ).resolves.toEqual(task)

    const [url, options] = fetchMock.mock.calls[0]

    expect(url).toBe('/api/tasks')
    expect(options.method).toBe('POST')
    expect(JSON.parse(options.body as string)).toEqual(input)
  })

  it('updates a task through the dedicated resource endpoint', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse({
        ...task,
        status: 'REVIEW',
      }),
    )

    await updateTask(
      12,
      {
        ...input,
        status: 'REVIEW',
      },
      session,
    )

    const [url, options] = fetchMock.mock.calls[0]

    expect(url).toBe('/api/tasks/12')
    expect(options.method).toBe('PUT')
    expect(JSON.parse(options.body as string)).toEqual({
      ...input,
      status: 'REVIEW',
    })
  })

  it('deletes a task through the resource endpoint', async () => {
    fetchMock.mockResolvedValue(
      new Response(null, { status: 204 }),
    )

    await expect(
      deleteTask(12, session),
    ).resolves.toBeUndefined()

    const [url, options] = fetchMock.mock.calls[0]

    expect(url).toBe('/api/tasks/12')
    expect(options.method).toBe('DELETE')
  })
})
