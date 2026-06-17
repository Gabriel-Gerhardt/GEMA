import { NavLink } from 'react-router-dom'
import { SunflowerWordmark } from './Logo'

const NAV_LINKS = [
  { to: '/', label: 'Home' },
  { to: '/qr/gallery', label: 'Gallery' },
  { to: '/profile', label: 'Profile' },
]

export default function Header() {
  return (
    <header className="border-b border-border-warm-200 bg-base-white">
      <div className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-3 px-4 py-3">
        <NavLink to="/">
          <SunflowerWordmark />
        </NavLink>
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
      </div>
    </header>
  )
}
