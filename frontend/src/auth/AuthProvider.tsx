import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from 'react'
import { UNAUTHORIZED_EVENT } from '../api/apiClient'
import type { AuthSession, LoginResponse } from '../types/auth'
import { AuthContext } from './AuthContext'
import {
  clearAuthSession,
  createAuthSession,
  readAuthSession,
  saveAuthSession,
} from './authStorage'

export function AuthProvider({ children }: PropsWithChildren) {
  const [session, setSession] = useState<AuthSession | null>(
    readAuthSession,
  )

  const signIn = useCallback((response: LoginResponse) => {
    const nextSession = createAuthSession(response)

    saveAuthSession(nextSession)
    setSession(nextSession)
  }, [])

  const signOut = useCallback(() => {
    clearAuthSession()
    setSession(null)
  }, [])

  useEffect(() => {
    function handleUnauthorizedSession() {
      signOut()
    }

    window.addEventListener(
      UNAUTHORIZED_EVENT,
      handleUnauthorizedSession,
    )

    return () => {
      window.removeEventListener(
        UNAUTHORIZED_EVENT,
        handleUnauthorizedSession,
      )
    }
  }, [signOut])

  useEffect(() => {
    if (!session) {
      return
    }

    const remainingTime = Math.max(
      session.expiresAt - Date.now(),
      0,
    )

    const timeoutId = window.setTimeout(() => {
      signOut()
    }, remainingTime)

    return () => {
      window.clearTimeout(timeoutId)
    }
  }, [session, signOut])

  const value = useMemo(
    () => ({
      session,
      isAuthenticated: session !== null,
      signIn,
      signOut,
    }),
    [session, signIn, signOut],
  )

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}