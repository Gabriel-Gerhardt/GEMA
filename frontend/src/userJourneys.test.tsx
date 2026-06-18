import { cleanup, render, screen, within } from '@testing-library/react'
import { fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it } from 'vitest'
import App from './App'

afterEach(cleanup)

function renderAppAt(initialPath: string) {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <App />
    </MemoryRouter>,
  )
}

describe('user journeys', () => {
  it('clicking a gallery card navigates to the matching QrCodeDetail page with the right publicId threaded through', () => {
    renderAppAt('/qr/gallery')

    // "Event check-in" card maps to publicId def456 in QrCodeGallery's mock data.
    const card = screen.getByText(/event check-in/i)
    fireEvent.click(card)

    expect(
      screen.getByRole('heading', { name: /edit qr code/i }),
    ).toBeTruthy()
    expect(screen.getByText(/public id: def456/i)).toBeTruthy()
  })

  it('clicking a different gallery card threads its own distinct publicId', () => {
    renderAppAt('/qr/gallery')

    const card = screen.getByText(/product packaging/i)
    fireEvent.click(card)

    expect(screen.getByText(/public id: ghi789/i)).toBeTruthy()
  })

  it('the CreateAccount "See how GEMA works" link navigates to the Onboarding page at /welcome', () => {
    renderAppAt('/create-account')

    const link = screen.getByRole('link', { name: /see how gema works/i })
    fireEvent.click(link)

    expect(
      screen.getByRole('heading', { name: /welcome to gema/i }),
    ).toBeTruthy()
    // Onboarding renders under PublicLayout, so the authenticated nav from
    // CreateAccount's AppLayout should no longer be present after navigating.
    expect(screen.queryByRole('navigation')).toBeNull()
  })

  it('a brand-new visitor journey: lands on the public landing page at "/", reads "Quem somos", clicks "Começar", arrives at the authenticated Home with full nav, then can reach Gallery from there', () => {
    renderAppAt('/')

    // Step 1: arrives at the marketing page, not the dashboard.
    expect(screen.getByRole('heading', { name: 'Quem somos' })).toBeTruthy()
    expect(screen.queryByRole('navigation')).toBeNull()

    // Step 2: clicks the primary CTA ("Começar" -> /home). The label is
    // repeated (hero CTA + optional footer repeat), so disambiguate by href.
    const cta = screen.getAllByRole('link', { name: /começar/i })[0]
    expect(cta.getAttribute('href')).toBe('/home')
    fireEvent.click(cta)

    // Step 3: lands on the dashboard with the authenticated nav now visible.
    expect(screen.getByRole('heading', { name: /welcome to gema/i })).toBeTruthy()
    const nav = screen.getByRole('navigation')
    expect(within(nav).getByRole('link', { name: /gallery/i })).toBeTruthy()

    // Step 4: from there, the visitor can keep going deeper into the app —
    // confirms the landing page hand-off doesn't strand the user outside
    // the normal authenticated navigation flow.
    fireEvent.click(within(nav).getByRole('link', { name: /gallery/i }))
    expect(screen.getByRole('heading', { name: /qr code gallery/i })).toBeTruthy()
  })

  it('a returning-user journey: lands on "/", uses the secondary "Entrar" link instead of the CTA, and reaches the Login page directly (skipping the dashboard)', () => {
    renderAppAt('/')

    const loginLink = screen.getByRole('link', { name: /entrar/i })
    fireEvent.click(loginLink)

    expect(screen.getByRole('heading', { name: /log in/i })).toBeTruthy()
    // Never passed through Home/the dashboard on the way.
    expect(screen.queryByRole('heading', { name: /welcome to gema/i })).toBeNull()
  })
})
