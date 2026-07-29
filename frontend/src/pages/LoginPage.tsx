import { useState, type FormEvent } from 'react'
import {
  Navigate,
  useLocation,
  useNavigate,
} from 'react-router'
import { authenticate } from '../api/authApi'
import { ApiClientError } from '../api/apiClient'
import { useAuth } from '../auth/useAuth'

interface LoginLocationState {
  from?: string
}

export function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [formError, setFormError] = useState('')
  const [fieldErrors, setFieldErrors] = useState<
    Record<string, string>
  >({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  const { isAuthenticated, signIn } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()

  const locationState =
    location.state as LoginLocationState | null

  const requestedPath = locationState?.from

  const destination =
    requestedPath?.startsWith('/')
      ? requestedPath
      : '/dashboard'

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />
  }

  async function handleSubmit(
    event: FormEvent<HTMLFormElement>,
  ) {
    event.preventDefault()

    setFormError('')
    setFieldErrors({})
    setIsSubmitting(true)

    try {
      const response = await authenticate({
        email: email.trim(),
        password,
      })

      signIn(response)
      navigate(destination, { replace: true })
    } catch (error) {
      if (error instanceof ApiClientError) {
        setFormError(error.message)
        setFieldErrors(error.validationErrors)
      } else {
        setFormError(
          'The authentication service is currently unavailable.',
        )
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="page">
      <section className="card">
        <p className="eyebrow">DevFlow Hub</p>
        <h1>Sign in</h1>

        <p className="description">
          Access the DevFlow Hub workspace using your account.
        </p>

        {formError && (
          <p className="form-error" role="alert">
            {formError}
          </p>
        )}

        <form className="form" onSubmit={handleSubmit}>
          <label htmlFor="email">Email</label>

          <input
            id="email"
            name="email"
            type="email"
            value={email}
            placeholder="name@example.com"
            autoComplete="email"
            maxLength={150}
            required
            disabled={isSubmitting}
            aria-invalid={Boolean(fieldErrors.email)}
            aria-describedby={
              fieldErrors.email ? 'email-error' : undefined
            }
            onChange={(event) => setEmail(event.target.value)}
          />

          {fieldErrors.email && (
            <span
              id="email-error"
              className="field-error"
            >
              {fieldErrors.email}
            </span>
          )}

          <label htmlFor="password">Password</label>

          <input
            id="password"
            name="password"
            type="password"
            value={password}
            placeholder="Enter your password"
            autoComplete="current-password"
            maxLength={128}
            required
            disabled={isSubmitting}
            aria-invalid={Boolean(fieldErrors.password)}
            aria-describedby={
              fieldErrors.password
                ? 'password-error'
                : undefined
            }
            onChange={(event) =>
              setPassword(event.target.value)
            }
          />

          {fieldErrors.password && (
            <span
              id="password-error"
              className="field-error"
            >
              {fieldErrors.password}
            </span>
          )}

          <button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Signing in...' : 'Sign in'}
          </button>
        </form>
      </section>
    </main>
  )
}
