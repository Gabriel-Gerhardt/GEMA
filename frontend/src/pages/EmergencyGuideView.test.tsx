import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it } from 'vitest'
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

describe('EmergencyGuideView', () => {
  it('shows a loading state before the guide resolves', () => {
    renderAt('abc123')
    expect(screen.getByRole('status').textContent).toMatch(/loading/i)
  })

  it('renders the guide title and description when found and active', async () => {
    renderAt('abc123')
    await waitFor(() =>
      screen.getByText(/Jordan needs a little extra time and patience/i),
    )
    expect(screen.getByText(/hidden disability/i)).toBeTruthy()
  })

  it('shows calm not-found copy for an unknown or inactive public id', async () => {
    renderAt('does-not-exist')
    await waitFor(() => screen.getByText(/isn't active right now/i))
    // Should not show an alarming "error" heading.
    expect(screen.queryByText(/error/i)).toBeNull()
  })

  it('shows the same calm copy for an explicitly inactive guide', async () => {
    renderAt('inactive1')
    await waitFor(() => screen.getByText(/isn't active right now/i))
  })
})
