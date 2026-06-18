import { NavLink } from 'react-router-dom'
import { SunflowerWordmark } from './Logo'

const NAV_LINKS = [
  { to: '/home', label: 'Home' },
  { to: '/qr/gallery', label: 'Gallery' },
  { to: '/profile', label: 'Profile' },
]

export interface HeaderProgress {
  step: number
  total: number
}

export interface HeaderProps {
  /** When set, renders a "Step X of Y" indicator in place of the nav links
   * (e.g. for the Onboarding flow) instead of the usual Home/Gallery/Profile
   * nav. Informational only — no step-jumping. */
  progress?: HeaderProgress
}

export default function Header({ progress }: HeaderProps = {}) {
  return (
    <header className="border-b border-border-warm-200 bg-base-white">
      <div className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-3 px-4 py-3">
        <NavLink to="/home">
          <SunflowerWordmark />
        </NavLink>
        {progress ? (
          <div className="flex items-center gap-3" role="status">
            <span className="text-sm font-medium text-text-warm-600">
              Step {progress.step} of {progress.total}
            </span>
            <div className="flex gap-1.5">
              {Array.from({ length: progress.total }, (_, i) => (
                <span
                  key={i}
                  className={`h-1.5 w-6 rounded-full ${
                    i < progress.step ? 'bg-primary-green-dark' : 'bg-border-warm-200'
                  }`}
                />
              ))}
            </div>
          </div>
        ) : (
          <nav className="flex flex-wrap gap-4">
            {NAV_LINKS.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                className={({ isActive }) =>
                  `text-base font-medium ${
                    isActive ? 'text-text-warm-900' : 'text-text-warm-600'
                  } hover:text-text-warm-900`
                }
              >
                {link.label}
              </NavLink>
            ))}
          </nav>
        )}
      </div>
    </header>
  )
}
