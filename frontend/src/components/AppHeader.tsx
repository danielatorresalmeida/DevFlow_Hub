import { NavLink, useNavigate } from 'react-router'
import { useAuth } from '../auth/useAuth'

interface AppHeaderProps {
  title: string
  description: string
}

function getNavLinkClassName(isActive: boolean): string {
  return isActive
    ? 'app-nav-link app-nav-link--active'
    : 'app-nav-link'
}

export function AppHeader({
  title,
  description,
}: AppHeaderProps) {
  const { session, signOut } = useAuth()
  const navigate = useNavigate()

  if (!session) {
    return null
  }

  function handleSignOut() {
    signOut()
    navigate('/login', { replace: true })
  }

  return (
    <header className="app-header">
      <div className="app-header-main">
        <div>
          <p className="eyebrow">DevFlow Hub</p>
          <h1>{title}</h1>
          <p className="description app-header-description">
            {description}
          </p>
        </div>

        <div className="app-header-account">
          <div className="app-user">
            <strong>{session.collaborator.name}</strong>
            <span>{session.collaborator.role}</span>
          </div>

          <button
            className="secondary-button"
            type="button"
            onClick={handleSignOut}
          >
            Sign out
          </button>
        </div>
      </div>

      <nav className="app-nav" aria-label="Main navigation">
        <NavLink
          to="/dashboard"
          className={({ isActive }) => (
            getNavLinkClassName(isActive)
          )}
        >
          Dashboard
        </NavLink>

        <NavLink
          to="/projects"
          className={({ isActive }) => (
            getNavLinkClassName(isActive)
          )}
        >
          Projects
        </NavLink>

        <NavLink
          to="/tasks"
          className={({ isActive }) => (
            getNavLinkClassName(isActive)
          )}
        >
          Tasks
        </NavLink>
      </nav>
    </header>
  )
}