import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from 'react'
import { AuthContext } from './AuthContext'
import {
  clearAuthSession,
  createAuthSession,
  readAuthSession,
  saveAuthSession,
} from './authStorage'
import type { AuthSession, LoginResponse } from '../types/auth'

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
    if (!session) {
      return
    }

    const remainingTime = session.expiresAt - Date.now()

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
