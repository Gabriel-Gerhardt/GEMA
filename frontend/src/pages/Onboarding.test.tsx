// Unit test for Onboarding's 3-step state machine (GAB-29): mark+meaning ->
// step 1 (create a code) -> step 2 (share it) + final CTA. Covers both the
// uncontrolled (internal state) and controlled (parent-driven `step` prop,
// as App.tsx uses to sync the Header's progress indicator) usage.
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it } from 'vitest'
import Onboarding from './Onboarding'

afterEach(cleanup)

function renderOnboarding(props: Parameters<typeof Onboarding>[0] = {}) {
  return render(
    <MemoryRouter>
      <Onboarding {...props} />
    </MemoryRouter>,
  )
}

describe('Onboarding step flow', () => {
  it('starts on step 1 (mark + meaning) with no Back button', () => {
    renderOnboarding()
    expect(screen.getByRole('heading', { name: /welcome to gema/i })).toBeTruthy()
    expect(screen.queryByRole('button', { name: /back/i })).toBeNull()
    expect(screen.getByRole('button', { name: /next/i })).toBeTruthy()
  })

  it('advances through steps 2 and 3 on Next, and back again on Back', () => {
    renderOnboarding()

    fireEvent.click(screen.getByRole('button', { name: /next/i }))
    expect(screen.getByRole('heading', { name: /create a qr code/i })).toBeTruthy()

    fireEvent.click(screen.getByRole('button', { name: /next/i }))
    expect(screen.getByRole('heading', { name: /share it/i })).toBeTruthy()
    // Final step shows the CTA instead of Next.
    expect(screen.queryByRole('button', { name: /^next$/i })).toBeNull()
    expect(screen.getByRole('link', { name: /get started/i })).toBeTruthy()

    fireEvent.click(screen.getByRole('button', { name: /back/i }))
    expect(screen.getByRole('heading', { name: /create a qr code/i })).toBeTruthy()
  })

  it('the final step\'s "Get started" link points to /create-account', () => {
    renderOnboarding({ step: 3 })
    const link = screen.getByRole('link', { name: /get started/i })
    expect(link.getAttribute('href')).toBe('/create-account')
  })

  it('in controlled mode, calls onStepChange instead of only updating internal state', () => {
    let lastStep: number | undefined
    renderOnboarding({ step: 1, onStepChange: (s) => (lastStep = s) })
    fireEvent.click(screen.getByRole('button', { name: /next/i }))
    expect(lastStep).toBe(2)
  })
})
