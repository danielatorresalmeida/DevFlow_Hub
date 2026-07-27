import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'
import type {
  AuthSession,
  LoginResponse,
} from '../types/auth'
import {
  clearAuthSession,
  createAuthSession,
  readAuthSession,
  saveAuthSession,
} from './authStorage'

const STORAGE_KEY = 'devflow.auth.session'

const collaborator = {
  id: 7,
  name: 'Test Collaborator',
  email: 'test@example.com',
  role: 'DEVELOPER',
  active: true,
}

const loginResponse: LoginResponse = {
  accessToken: 'access-token',
  tokenType: 'Bearer',
  expiresIn: 120,
  collaborator,
}

function createValidSession(): AuthSession {
  return {
    accessToken: 'stored-token',
    tokenType: 'Bearer',
    expiresAt: Date.now() + 60_000,
    collaborator,
  }
}

describe('authStorage', () => {
  beforeEach(() => {
    sessionStorage.clear()
  })

  it('creates a session with an absolute expiration time', () => {
    vi.spyOn(Date, 'now').mockReturnValue(
      1_700_000_000_000,
    )

    const session = createAuthSession(loginResponse)

    expect(session).toEqual({
      accessToken: 'access-token',
      tokenType: 'Bearer',
      expiresAt: 1_700_000_120_000,
      collaborator,
    })
  })

  it('saves and reads a valid session', () => {
    const session = createValidSession()

    saveAuthSession(session)

    expect(
      JSON.parse(
        sessionStorage.getItem(STORAGE_KEY) ?? '',
      ),
    ).toEqual(session)

    expect(readAuthSession()).toEqual(session)
  })

  it('clears a stored session', () => {
    saveAuthSession(createValidSession())

    clearAuthSession()

    expect(
      sessionStorage.getItem(STORAGE_KEY),
    ).toBeNull()
  })

  it('rejects and removes an expired session', () => {
    const expiredSession: AuthSession = {
      ...createValidSession(),
      expiresAt: Date.now() - 1,
    }

    sessionStorage.setItem(
      STORAGE_KEY,
      JSON.stringify(expiredSession),
    )

    expect(readAuthSession()).toBeNull()

    expect(
      sessionStorage.getItem(STORAGE_KEY),
    ).toBeNull()
  })

  it('rejects and removes malformed JSON', () => {
    sessionStorage.setItem(
      STORAGE_KEY,
      '{invalid-json',
    )

    expect(readAuthSession()).toBeNull()

    expect(
      sessionStorage.getItem(STORAGE_KEY),
    ).toBeNull()
  })

  it('rejects and removes an incomplete session', () => {
    sessionStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({
        accessToken: 'incomplete-token',
      }),
    )

    expect(readAuthSession()).toBeNull()

    expect(
      sessionStorage.getItem(STORAGE_KEY),
    ).toBeNull()
  })
})