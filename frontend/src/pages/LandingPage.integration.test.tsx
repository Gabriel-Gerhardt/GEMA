// Integration/acceptance tests for the public landing page (GAB-30) as
// actually wired through App.tsx's routing, not the isolated component.
// LandingPage.test.tsx covers the component's own content in isolation
// (rendered standalone under a bare MemoryRouter). The gap this file closes:
// mounting the *real* App at "/" and following the actual user journeys the
// design doc describes —
//   - a visitor lands on "/" and sees the marketing page, not Home, with no
//     authenticated nav anywhere on screen;
//   - clicking the primary CTA actually routes to "/home" and the
//     authenticated Header/nav appears only after that navigation, not
//     before;
//   - clicking the secondary "Entrar" link actually routes to "/login";
//   - visiting "/home" directly (not via the CTA) renders the relocated
//     Home dashboard with a Header whose "Home" nav link and brand wordmark
//     both resolve to "/home" — re-verified here via real navigation, not
//     just a static href assertion, closing the regression this ticket
//     previously caught;
//   - there is no way to reach the internal app's nav (Gallery/Profile) from
//     "/" except through the explicit CTA/login links in the page content.
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it } from 'vitest'
import App from '../App'

afterEach(cleanup)

function renderAppAt(initialPath: string) {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <App />
    </MemoryRouter>,
  )
}

describe('LandingPage integration (via "/" route)', () => {
  it('a visitor landing on "/" sees the marketing page with no authenticated nav at all', () => {
    renderAppAt('/')

    expect(screen.getByRole('heading', { level: 1 })).toBeTruthy()
    expect(screen.getByRole('heading', { name: 'Quem somos', level: 2 })).toBeTruthy()
    // No nav landmark, and specifically none of the authenticated app's
    // internal links (Gallery/Profile) anywhere on the page.
    expect(screen.queryByRole('navigation')).toBeNull()
    expect(screen.queryByRole('link', { name: /gallery/i })).toBeNull()
    expect(screen.queryByRole('link', { name: /profile/i })).toBeNull()
  })

  it('the only links out of "/" are the explicit CTA (-> /home) and login affordance (-> /login)', () => {
    renderAppAt('/')

    const hrefs = screen.getAllByRole('link').map((link) => link.getAttribute('href'))
    // Every link on the landing page must resolve to one of these two
    // destinations — nothing else, and in particular no implicit route into
    // Gallery/Profile/etc.
    for (const href of hrefs) {
      expect(['/home', '/login']).toContain(href)
    }
    expect(hrefs).toContain('/home')
    expect(hrefs).toContain('/login')
  })

  it('clicking the primary CTA navigates from "/" to "/home" and the authenticated Header/nav now appears', () => {
    renderAppAt('/')

    // Disambiguate the primary CTA (rendered as a Button, not a plain text
    // link) from the secondary "Entrar" text link, both of which point
    // elsewhere/at "/home" in this page.
    const ctaLink = screen.getAllByRole('link').find((link) => link.getAttribute('href') === '/home')
    expect(ctaLink).toBeTruthy()

    // Before navigating: definitely no nav landmark yet.
    expect(screen.queryByRole('navigation')).toBeNull()

    fireEvent.click(ctaLink as HTMLElement)

    // After navigating: Home's dashboard content and the authenticated
    // Header/nav are now present; the landing page content is gone.
    expect(screen.getByRole('heading', { name: /welcome to gema/i })).toBeTruthy()
    expect(screen.queryByRole('heading', { name: 'Quem somos' })).toBeNull()
    const nav = screen.getByRole('navigation')
    expect(within(nav).getByRole('link', { name: /gallery/i })).toBeTruthy()
    expect(within(nav).getByRole('link', { name: /profile/i })).toBeTruthy()
  })

  it('clicking the secondary "Entrar" link navigates from "/" to "/login"', () => {
    renderAppAt('/')

    const loginLink = screen.getByRole('link', { name: /entrar/i })
    expect(loginLink.getAttribute('href')).toBe('/login')

    fireEvent.click(loginLink)

    expect(screen.getByRole('heading', { name: /log in/i })).toBeTruthy()
  })

  it('visiting "/home" directly (not via the landing page) renders Home under the authenticated Header, with the nav Home link and wordmark both resolving to "/home"', () => {
    renderAppAt('/home')

    expect(screen.getByRole('heading', { name: /welcome to gema/i })).toBeTruthy()
    const nav = screen.getByRole('navigation')

    // Regression re-check end-to-end: clicking the Header's own "Home" nav
    // link while already on /home must keep the user on /home (not bounce
    // them out to the public landing page at "/").
    fireEvent.click(within(nav).getByRole('link', { name: /home/i }))
    expect(screen.getByRole('heading', { name: /welcome to gema/i })).toBeTruthy()
    expect(screen.queryByRole('heading', { name: 'Quem somos' })).toBeNull()

    // Same re-check for the brand wordmark link.
    const wordmarkLink = screen.getByRole('link', { name: /gema/i })
    fireEvent.click(wordmarkLink)
    expect(screen.getByRole('heading', { name: /welcome to gema/i })).toBeTruthy()
    expect(screen.queryByRole('heading', { name: 'Quem somos' })).toBeNull()
  })

  it('navigating to "/home" then to "/" via direct history entries shows the right shell each time (no stale nav leaking between them)', () => {
    const view = renderAppAt('/home')
    expect(screen.getByRole('navigation')).toBeTruthy()
    view.unmount()

    renderAppAt('/')
    expect(screen.queryByRole('navigation')).toBeNull()
    expect(screen.getByRole('heading', { level: 1 })).toBeTruthy()
  })
})
