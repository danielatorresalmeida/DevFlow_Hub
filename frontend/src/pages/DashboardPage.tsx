import { Link } from 'react-router'

export function DashboardPage() {
  return (
    <main className="page">
      <section className="card card--wide">
        <p className="eyebrow">DevFlow Hub</p>
        <h1>Dashboard</h1>

        <p className="description">
          The React frontend foundation is running successfully.
        </p>

        <div className="dashboard-grid">
          <article className="dashboard-item">
            <strong>Projects</strong>
            <span>Project management will be added here.</span>
          </article>

          <article className="dashboard-item">
            <strong>Tasks</strong>
            <span>Task tracking will be added here.</span>
          </article>

          <article className="dashboard-item">
            <strong>Collaborators</strong>
            <span>Collaborator management will be added here.</span>
          </article>
        </div>

        <Link className="text-link" to="/login">
          Return to login
        </Link>
      </section>
    </main>
  )
}
