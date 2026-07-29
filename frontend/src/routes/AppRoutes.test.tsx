import {
  render,
  screen,
} from '@testing-library/react'
import {
  MemoryRouter,
} from 'react-router'
import {
  describe,
  expect,
  it,
  vi,
} from 'vitest'
import {
  AuthContext,
  type AuthContextValue,
} from '../auth/AuthContext'
import type { AuthSession } from '../types/auth'
import { AppRoutes } from './AppRoutes'

vi.mock('../pages/DashboardPage', () => ({
  DashboardPage: () => (
    <h1>Dashboard page</h1>
  ),
}))

vi.mock('../pages/ProjectsPage', () => ({
  ProjectsPage: () => (
    <h1>Projects page</h1>
  ),
}))

vi.mock('../pages/ProjectDetailPage', () => ({
  ProjectDetailPage: () => (
    <h1>Project detail page</h1>
  ),
}))

vi.mock('../pages/TasksPage', () => ({
  TasksPage: () => (
    <h1>Tasks page</h1>
  ),
}))

vi.mock('../pages/TaskDetailPage', () => ({
  TaskDetailPage: () => (
    <h1>Task detail page</h1>
  ),
}))

const authenticatedSession: AuthSession = {
  accessToken: 'access-token',
  tokenType: 'Bearer',
  expiresAt: Date.now() + 60_000,
  collaborator: {
    id: 7,
    name: 'Test Collaborator',
    email: 'test@example.com',
    role: 'DEVELOPER',
    active: true,
  },
}

function createAuthValue(
  session: AuthSession | null,
): AuthContextValue {
  return {
    session,
    isAuthenticated: session !== null,
    signIn: () => undefined,
    signOut: () => undefined,
  }
}

function renderAppRoutes(
  path: string,
  session: AuthSession | null,
) {
  render(
    <MemoryRouter initialEntries={[path]}>
      <AuthContext.Provider
        value={createAuthValue(session)}
      >
        <AppRoutes />
      </AuthContext.Provider>
    </MemoryRouter>,
  )
}

describe('AppRoutes', () => {
  it('redirects unauthenticated root access to login', async () => {
    renderAppRoutes('/', null)

    expect(
      await screen.findByRole('heading', {
        name: 'Sign in',
      }),
    ).toBeInTheDocument()
  })

  it('redirects authenticated root access to dashboard', async () => {
    renderAppRoutes(
      '/',
      authenticatedSession,
    )

    expect(
      await screen.findByRole('heading', {
        name: 'Dashboard page',
      }),
    ).toBeInTheDocument()
  })

  it('redirects authenticated login access to dashboard', async () => {
    renderAppRoutes(
      '/login',
      authenticatedSession,
    )

    expect(
      await screen.findByRole('heading', {
        name: 'Dashboard page',
      }),
    ).toBeInTheDocument()
  })

  it.each([
    ['/projects', 'Projects page'],
    ['/projects/42', 'Project detail page'],
    ['/tasks', 'Tasks page'],
    ['/tasks/42', 'Task detail page'],
  ])(
    'renders %s for authenticated users',
    async (path, heading) => {
      renderAppRoutes(
        path,
        authenticatedSession,
      )

      expect(
        await screen.findByRole('heading', {
          name: heading,
        }),
      ).toBeInTheDocument()
    },
  )

  it('renders the Not Found page for an unknown route', () => {
    renderAppRoutes(
      '/route-that-does-not-exist',
      null,
    )

    expect(
      screen.getByRole('heading', {
        name: 'Page not found',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByRole('link', {
        name: 'Return to login',
      }),
    ).toHaveAttribute(
      'href',
      '/login',
    )
  })
})