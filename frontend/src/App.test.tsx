import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'

afterEach(cleanup)

function renderAppAt(initialPath: string) {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <App />
    </MemoryRouter>,
  )
}

describe('App routing', () => {
  it('mounts Home with the authenticated Header/nav at "/"', () => {
    renderAppAt('/')
    // Header brand wordmark + nav links should be present.
    const nav = screen.getByRole('navigation')
    expect(nav).toBeTruthy()
    expect(within(nav).getByRole('link', { name: /gallery/i })).toBeTruthy()
    expect(within(nav).getByRole('link', { name: /profile/i })).toBeTruthy()
  })

  it('mounts QrCodeGallery at "/qr/gallery" with the Header present', () => {
    renderAppAt('/qr/gallery')
    expect(screen.getByRole('heading', { name: /qr code gallery/i })).toBeTruthy()
    expect(screen.getByRole('navigation')).toBeTruthy()
  })

  it('mounts QrCodeDetail at "/qr/:publicId/edit" threading the publicId param, with the Header present', () => {
    renderAppAt('/qr/abc123/edit')
    expect(screen.getByRole('heading', { name: /edit qr code/i })).toBeTruthy()
    expect(screen.getByText(/public id: abc123/i)).toBeTruthy()
    // QrCodeDetail is wired under AppLayout in App.tsx, so the Header/nav
    // is expected to render here (unlike the public, unauthenticated routes).
    expect(screen.getByRole('navigation')).toBeTruthy()
  })

  it('mounts Onboarding at "/welcome" via PublicLayout, with no Header/nav', () => {
    renderAppAt('/welcome')
    expect(screen.getByRole('heading', { name: /welcome to gema/i })).toBeTruthy()
    expect(screen.queryByRole('navigation')).toBeNull()
    expect(screen.queryByRole('link', { name: /gallery/i })).toBeNull()
  })

  it('mounts EmergencyGuideView at "/q/:publicId" via PublicLayout, with no Header/nav, and resolves the guide for the param', async () => {
    vi.stubGlobal('fetch', vi.fn())
    vi.mocked(fetch).mockReturnValue(
      Promise.resolve(
        new Response(
          JSON.stringify({
            publicId: 'abc123',
            title: 'Jordan needs a little extra time and patience',
            description: 'I have a hidden disability.',
            isActive: true,
            createdAt: '2026-06-01',
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        ),
      ),
    )

    renderAppAt('/q/abc123')
    // Loading state first.
    expect(screen.getByRole('status').textContent).toMatch(/loading/i)
    expect(screen.queryByRole('navigation')).toBeNull()

    await waitFor(() =>
      screen.getByText(/Jordan needs a little extra time and patience/i),
    )
    expect(screen.queryByRole('navigation')).toBeNull()

    vi.unstubAllGlobals()
  })

  it('falls back to NotFound via PublicLayout for an unmatched path, with no Header/nav', () => {
    renderAppAt('/this-route-does-not-exist')
    expect(screen.getByRole('heading', { name: /page not found/i })).toBeTruthy()
    expect(screen.queryByRole('navigation')).toBeNull()
  })
})
