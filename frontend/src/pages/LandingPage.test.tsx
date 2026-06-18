// Unit test for the public landing page (GAB-30): hero, "Quem somos", and
// product explanation sections, plus the primary/secondary CTAs.
import { cleanup, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it } from 'vitest'
import LandingPage from './LandingPage'

afterEach(cleanup)

function renderLandingPage() {
  return render(
    <MemoryRouter>
      <LandingPage />
    </MemoryRouter>,
  )
}

describe('LandingPage', () => {
  it('renders the hero heading', () => {
    renderLandingPage()
    expect(screen.getByRole('heading', { level: 1 })).toBeTruthy()
  })

  it('renders the "Quem somos" section with the exact heading text', () => {
    renderLandingPage()
    expect(screen.getByRole('heading', { name: 'Quem somos', level: 2 })).toBeTruthy()
  })

  it('renders the product explanation section heading', () => {
    renderLandingPage()
    const h2s = screen.getAllByRole('heading', { level: 2 })
    expect(h2s.length).toBeGreaterThanOrEqual(2)
  })

  it('renders a primary CTA link to /home', () => {
    renderLandingPage()
    const links = screen.getAllByRole('link').filter((l) => l.getAttribute('href') === '/home')
    expect(links.length).toBeGreaterThan(0)
  })

  it('renders a secondary "Entrar" link to /login', () => {
    renderLandingPage()
    const link = screen.getByRole('link', { name: /entrar/i })
    expect(link.getAttribute('href')).toBe('/login')
  })
})
