import { cleanup, render, screen } from '@testing-library/react'
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
})
