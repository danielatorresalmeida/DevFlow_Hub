import type { AuthSession, LoginResponse } from '../types/auth'

const STORAGE_KEY = 'devflow.auth.session'

export function createAuthSession(
  response: LoginResponse,
): AuthSession {
  return {
    accessToken: response.accessToken,
    tokenType: response.tokenType,
    expiresAt: Date.now() + response.expiresIn * 1000,
    collaborator: response.collaborator,
  }
}

export function saveAuthSession(session: AuthSession): void {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session))
}

export function clearAuthSession(): void {
  sessionStorage.removeItem(STORAGE_KEY)
}

export function readAuthSession(): AuthSession | null {
  const storedSession = sessionStorage.getItem(STORAGE_KEY)

  if (!storedSession) {
    return null
  }

  try {
    const session = JSON.parse(storedSession) as AuthSession

    if (
      !session.accessToken ||
      !session.tokenType ||
      !session.collaborator ||
      session.expiresAt <= Date.now()
    ) {
      clearAuthSession()
      return null
    }

    return session
  } catch {
    clearAuthSession()
    return null
  }
}
