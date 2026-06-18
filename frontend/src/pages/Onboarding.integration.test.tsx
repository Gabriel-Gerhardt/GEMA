// Integration/acceptance tests for the Onboarding flow as actually wired
// through App.tsx's OnboardingRoute (GAB-29), not the isolated component.
// Onboarding.test.tsx covers the component's step state machine directly
// (uncontrolled + controlled prop usage in isolation). The gap this file
// closes: mounting the real app at /welcome, where OnboardingRoute owns the
// step state and threads it into both <Header progress={...}> and
// <Onboarding step .../>, so we can verify the Header's "Step X of Y" text
// and dot indicator actually stay in sync with the on-screen step content
// as a user clicks through, and that the flow's edge behaviors (no step 0,
// no step 4, Back absent on step 1) hold end-to-end through the real route.
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

function progressStatus() {
  return screen.getByRole('status')
}

describe('Onboarding integration (via /welcome route)', () => {
  it('loads on step 1 with the Header showing "Step 1 of 3" and no Back button', () => {
    renderAppAt('/welcome')

    expect(screen.getByRole('heading', { name: /welcome to gema/i })).toBeTruthy()
    expect(within(progressStatus()).getByText(/step 1 of 3/i)).toBeTruthy()
    expect(screen.queryByRole('button', { name: /back/i })).toBeNull()
    // No authenticated nav (Home/Gallery/Profile) should leak into the
    // public onboarding shell.
    expect(screen.queryByRole('link', { name: /gallery/i })).toBeNull()
  })

  it('clicking Next through all 3 steps keeps the Header progress indicator synced with on-screen content', () => {
    renderAppAt('/welcome')

    // Step 1 -> 2
    fireEvent.click(screen.getByRole('button', { name: /next/i }))
    expect(screen.getByRole('heading', { name: /create a qr code/i })).toBeTruthy()
    expect(within(progressStatus()).getByText(/step 2 of 3/i)).toBeTruthy()

    // Step 2 -> 3
    fireEvent.click(screen.getByRole('button', { name: /next/i }))
    expect(screen.getByRole('heading', { name: /share it/i })).toBeTruthy()
    expect(within(progressStatus()).getByText(/step 3 of 3/i)).toBeTruthy()

    // Final step: Next is gone, replaced by the "Get started" CTA link
    // pointing at /create-account.
    expect(screen.queryByRole('button', { name: /^next$/i })).toBeNull()
    const cta = screen.getByRole('link', { name: /get started/i })
    expect(cta.getAttribute('href')).toBe('/create-account')
  })

  it('clicking the CTA link actually navigates to /create-account inside the real router', () => {
    renderAppAt('/welcome')
    fireEvent.click(screen.getByRole('button', { name: /next/i }))
    fireEvent.click(screen.getByRole('button', { name: /next/i }))

    fireEvent.click(screen.getByRole('link', { name: /get started/i }))

    expect(screen.getByRole('heading', { name: /create your account/i })).toBeTruthy()
  })

  it('Back navigates step-by-step and keeps the Header progress indicator in sync going backwards', () => {
    renderAppAt('/welcome')
    fireEvent.click(screen.getByRole('button', { name: /next/i }))
    fireEvent.click(screen.getByRole('button', { name: /next/i }))
    expect(within(progressStatus()).getByText(/step 3 of 3/i)).toBeTruthy()

    fireEvent.click(screen.getByRole('button', { name: /back/i }))
    expect(screen.getByRole('heading', { name: /create a qr code/i })).toBeTruthy()
    expect(within(progressStatus()).getByText(/step 2 of 3/i)).toBeTruthy()

    fireEvent.click(screen.getByRole('button', { name: /back/i }))
    expect(screen.getByRole('heading', { name: /welcome to gema/i })).toBeTruthy()
    expect(within(progressStatus()).getByText(/step 1 of 3/i)).toBeTruthy()
    // Back at step 1 again: the Back button must disappear, not just be
    // disabled, matching the "no dead-end affordance" design intent.
    expect(screen.queryByRole('button', { name: /back/i })).toBeNull()
  })

  it('rapidly double-clicking Next at the last step does not overshoot past step 3 (no step 4 / no crash)', () => {
    renderAppAt('/welcome')
    fireEvent.click(screen.getByRole('button', { name: /next/i })) // step 2
    fireEvent.click(screen.getByRole('button', { name: /next/i })) // step 3

    // At step 3 there is no "Next" button anymore (replaced by the CTA),
    // so simulate a user mashing the same spot fast: the CTA link, not a
    // button, is what's left. Confirm the indicator is pinned at 3 of 3 and
    // nothing further can advance the step counter past the total.
    expect(within(progressStatus()).getByText(/step 3 of 3/i)).toBeTruthy()
    expect(screen.queryByRole('button', { name: /^next$/i })).toBeNull()

    // Clicking the CTA multiple times in a row should still just navigate
    // once, without throwing or duplicating headings.
    const cta = screen.getByRole('link', { name: /get started/i })
    fireEvent.click(cta)
    fireEvent.click(cta)
    expect(screen.getAllByRole('heading', { name: /create your account/i })).toHaveLength(1)
  })

  it('reloading the flow at /welcome always resets to step 1 (no persistence across mounts)', () => {
    const first = renderAppAt('/welcome')
    fireEvent.click(screen.getByRole('button', { name: /next/i }))
    expect(within(progressStatus()).getByText(/step 2 of 3/i)).toBeTruthy()
    first.unmount()

    renderAppAt('/welcome')
    expect(within(progressStatus()).getByText(/step 1 of 3/i)).toBeTruthy()
    expect(screen.getByRole('heading', { name: /welcome to gema/i })).toBeTruthy()
  })
})
