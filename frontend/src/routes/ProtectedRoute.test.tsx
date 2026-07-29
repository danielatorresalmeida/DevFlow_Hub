import {
  render,
  screen,
} from '@testing-library/react'
import {
  MemoryRouter,
  Route,
  Routes,
  useLocation,
} from 'react-router'
import {
  describe,
  expect,
  it,
} from 'vitest'
import {
  AuthContext,
  type AuthContextValue,
} from '../auth/AuthContext'
import type { AuthSession } from '../types/auth'
import { ProtectedRoute } from './ProtectedRoute'

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

function LocationProbe() {
  const location = useLocation()

  return (
    <output data-testid="location">
      {JSON.stringify({
        pathname: location.pathname,
        state: location.state,
      })}
    </output>
  )
}

function renderProtectedRoute(
  authValue: AuthContextValue,
) {
  render(
    <MemoryRouter initialEntries={['/private']}>
      <AuthContext.Provider value={authValue}>
        <Routes>
          <Route
            path="/login"
            element={<LocationProbe />}
          />

          <Route
            path="/private"
            element={
              <ProtectedRoute>
                <h1>Private content</h1>
              </ProtectedRoute>
            }
          />
        </Routes>
      </AuthContext.Provider>
    </MemoryRouter>,
  )
}

describe('ProtectedRoute', () => {
  it('redirects unauthenticated users and preserves the requested path', async () => {
    renderProtectedRoute(
      createAuthValue(null),
    )

    const location = await screen.findByTestId(
      'location',
    )

    expect(location).toHaveTextContent(
      '"pathname":"/login"',
    )

    expect(location).toHaveTextContent(
      '"from":"/private"',
    )

    expect(
      screen.queryByRole('heading', {
        name: 'Private content',
      }),
    ).not.toBeInTheDocument()
  })

  it('renders protected content for authenticated users', () => {
    renderProtectedRoute(
      createAuthValue(authenticatedSession),
    )

    expect(
      screen.getByRole('heading', {
        name: 'Private content',
      }),
    ).toBeInTheDocument()

    expect(
      screen.queryByTestId('location'),
    ).not.toBeInTheDocument()
  })
})