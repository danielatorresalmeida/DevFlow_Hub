import { useNavigate } from 'react-router'
import { useAuth } from '../auth/useAuth'

export function DashboardPage() {
  const { session, signOut } = useAuth()
  const navigate = useNavigate()

  if (!session) {
    return null
  }

  const { collaborator } = session

  function handleSignOut() {
    signOut()
    navigate('/login', { replace: true })
  }

  return (
    <main className="page">
      <section className="card card--wide">
        <header className="dashboard-header">
          <div>
            <p className="eyebrow">DevFlow Hub</p>
            <h1>Dashboard</h1>
          </div>

          <button
            className="secondary-button"
            type="button"
            onClick={handleSignOut}
          >
            Sign out
          </button>
        </header>

        <p className="description">
          Welcome, {collaborator.name}.
        </p>

        <section
          className="account-panel"
          aria-labelledby="account-heading"
        >
          <h2 id="account-heading">Authenticated account</h2>

          <dl className="account-details">
            <div>
              <dt>Name</dt>
              <dd>{collaborator.name}</dd>
            </div>

            <div>
              <dt>Email</dt>
              <dd>{collaborator.email}</dd>
            </div>

            <div>
              <dt>Role</dt>
              <dd>{collaborator.role}</dd>
            </div>

            <div>
              <dt>Status</dt>
              <dd>
                {collaborator.active ? 'Active' : 'Inactive'}
              </dd>
            </div>
          </dl>
        </section>

        <div className="dashboard-grid">
          <article className="dashboard-item">
            <strong>Projects</strong>
            <span>
              Project management will be added here.
            </span>
          </article>

          <article className="dashboard-item">
            <strong>Tasks</strong>
            <span>
              Task tracking will be added here.
            </span>
          </article>

          <article className="dashboard-item">
            <strong>Collaborators</strong>
            <span>
              Collaborator management will be added here.
            </span>
          </article>
        </div>
      </section>
    </main>
  )
}
