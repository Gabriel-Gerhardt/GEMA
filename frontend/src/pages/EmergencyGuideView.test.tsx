import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import EmergencyGuideView from './EmergencyGuideView'

afterEach(cleanup)

function renderAt(publicId: string) {
  return render(
    <MemoryRouter initialEntries={[`/q/${publicId}`]}>
      <Routes>
        <Route path="/q/:publicId" element={<EmergencyGuideView />} />
      </Routes>
    </MemoryRouter>,
  )
}

function jsonResponse(status: number, body: unknown) {
  return Promise.resolve(
    new Response(JSON.stringify(body), {
      status,
      headers: { 'Content-Type': 'application/json' },
    }),
  )
}

describe('EmergencyGuideView', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  it('shows a loading state before the guide resolves', () => {
    vi.mocked(fetch).mockReturnValue(new Promise(() => {}))
    renderAt('abc123')
    expect(screen.getByRole('status').textContent).toMatch(/loading/i)
  })

  it('renders the guide title and description when found and active', async () => {
    vi.mocked(fetch).mockReturnValue(
      jsonResponse(200, {
        publicId: 'abc123',
        title: 'Jordan needs a little extra time and patience',
        description: 'I have a hidden disability.',
        isActive: true,
        createdAt: '2026-06-01',
      }),
    )
    renderAt('abc123')
    await waitFor(() =>
      screen.getByText(/Jordan needs a little extra time and patience/i),
    )
    expect(screen.getByText(/hidden disability/i)).toBeTruthy()
  })

  it('shows calm not-found copy for a 404 public id', async () => {
    vi.mocked(fetch).mockReturnValue(
      jsonResponse(404, {
        description: 'Not found',
        message: 'QR code not found',
        httpStatus: 404,
      }),
    )
    renderAt('does-not-exist')
    await waitFor(() => screen.getByText(/isn't active right now/i))
    expect(screen.queryByText(/we hit a snag/i)).toBeNull()
  })

  it('shows the same calm copy for an explicitly inactive guide', async () => {
    vi.mocked(fetch).mockReturnValue(
      jsonResponse(200, {
        publicId: 'inactive1',
        title: 'Retired guide',
        description: 'This guide has been turned off.',
        isActive: false,
        createdAt: '2026-01-10',
      }),
    )
    renderAt('inactive1')
    await waitFor(() => screen.getByText(/isn't active right now/i))
  })

  it('shows an error state with retry on network failure, recovering on retry', async () => {
    vi.mocked(fetch).mockRejectedValueOnce(new TypeError('network error'))
    renderAt('abc123')
    await waitFor(() => screen.getByText(/we hit a snag/i))

    vi.mocked(fetch).mockReturnValue(
      jsonResponse(200, {
        publicId: 'abc123',
        title: 'Jordan needs a little extra time and patience',
        description: 'I have a hidden disability.',
        isActive: true,
        createdAt: '2026-06-01',
      }),
    )
    screen.getByRole('button', { name: /try again/i }).click()

    await waitFor(() =>
      screen.getByText(/Jordan needs a little extra time and patience/i),
    )
  })
})
