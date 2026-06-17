import { NavLink } from 'react-router-dom'

const NAV_LINKS = [
  { to: '/', label: 'Home' },
  { to: '/qr/gallery', label: 'Gallery' },
  { to: '/profile', label: 'Profile' },
]

export default function Header() {
  return (
    <header className="border-b border-border-gray-200 bg-base-white">
      <div className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-3 px-4 py-3">
        <NavLink to="/" className="text-xl font-bold text-text-gray-900">
          GEMA
        </NavLink>
        <nav className="flex flex-wrap gap-4">
          {NAV_LINKS.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                `text-base font-medium ${
                  isActive ? 'text-text-gray-900' : 'text-text-gray-600'
                } hover:text-text-gray-900`
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
