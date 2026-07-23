import { Link } from 'react-router'

export function LoginPage() {
  return (
    <main className="page">
      <section className="card">
        <p className="eyebrow">DevFlow Hub</p>
        <h1>Sign in</h1>

        <p className="description">
          Access the DevFlow Hub workspace using your account.
        </p>

        <form className="form">
          <label htmlFor="email">Email</label>
          <input
            id="email"
            name="email"
            type="email"
            placeholder="name@example.com"
            autoComplete="email"
            disabled
          />

          <label htmlFor="password">Password</label>
          <input
            id="password"
            name="password"
            type="password"
            placeholder="Enter your password"
            autoComplete="current-password"
            disabled
          />

          <button type="button" disabled>
            Sign in
          </button>
        </form>

        <p className="temporary-note">
          Authentication integration will be added in the next stage.
        </p>

        <Link className="text-link" to="/dashboard">
          View temporary dashboard
        </Link>
      </section>
    </main>
  )
}
