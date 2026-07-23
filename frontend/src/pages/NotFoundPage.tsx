import { Link } from 'react-router'

export function NotFoundPage() {
  return (
    <main className="page">
      <section className="card">
        <p className="eyebrow">Error 404</p>
        <h1>Page not found</h1>

        <p className="description">
          The requested page does not exist.
        </p>

        <Link className="text-link" to="/login">
          Return to login
        </Link>
      </section>
    </main>
  )
}
