// Regression coverage for the Card variant switch introduced in GAB-29:
// EmergencyGuideView's `ready` state now renders inside `Card variant="plain"`
// (full-bleed, no border/shadow/padding classes) while `loading`/`error`/
// `unavailable` keep the original boxed `Card` (bordered/shadowed/padded).
// EmergencyGuideView.test.tsx and .integration.test.tsx already cover the
// state machine's data/request behavior; this file specifically asserts the
// *container* (Card variant) is correct per state, and — the actual gap —
// that transitioning directly from a boxed state (unavailable/error) into
// the ready state on a retry correctly swaps from the boxed container to the
// plain one rather than leaving stale boxed styling behind.
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

// The boxed Card variant's signature class (asymmetric corner + border);
// the plain variant renders with none of these classes (see Card.tsx's
// VARIANT_CLASSES map: 'plain' -> '').
const BOXED_SIGNATURE_CLASS = 'border-border-warm-200'

describe('EmergencyGuideView Card variant per state', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  it('renders the unavailable state in a boxed Card', async () => {
    vi.mocked(fetch).mockReturnValue(
      jsonResponse(404, { description: 'Not found', message: 'x', httpStatus: 404 }),
    )
    renderAt('missing')
    const heading = await screen.findByRole('heading', { name: /isn't active right now/i })
    const card = heading.parentElement as HTMLElement
    expect(card.className).toContain(BOXED_SIGNATURE_CLASS)
  })

  it('renders the error state in a boxed Card (status role container)', async () => {
    vi.mocked(fetch).mockRejectedValueOnce(new TypeError('network error'))
    renderAt('abc123')
    await waitFor(() => screen.getByText(/we hit a snag/i))
    // ErrorState itself is rendered inside the boxed <main> wrapper, not a
    // Card — confirm no plain/full-bleed styling leaks in by checking the
    // retry affordance renders inside the bounded layout (max-w-md, not
    // max-w-prose used by the full-bleed ready state).
    const main = screen.getByRole('button', { name: /try again/i }).closest('main')
    expect(main?.className).toContain('max-w-md')
  })

  it('renders the ready state full-bleed: no boxed border classes, and the wider max-w-prose container', async () => {
    vi.mocked(fetch).mockReturnValue(
      jsonResponse(200, {
        publicId: 'abc123',
        title: 'Ready guide',
        description: 'Body text.',
        isActive: true,
        createdAt: '2026-06-01',
      }),
    )
    renderAt('abc123')
    const heading = await screen.findByRole('heading', { name: /ready guide/i })
    const card = heading.parentElement as HTMLElement
    expect(card.className).not.toContain(BOXED_SIGNATURE_CLASS)

    const main = heading.closest('main')
    expect(main?.className).toContain('max-w-prose')
  })

  it('the unavailable state has no retry affordance (it is a terminal calm message, not a retryable error) — confirming it cannot itself transition to ready in the UI', async () => {
    vi.mocked(fetch).mockReturnValueOnce(
      jsonResponse(200, {
        publicId: 'abc123',
        title: 'Paused guide',
        description: 'paused',
        isActive: false,
        createdAt: '2026-06-01',
      }),
    )
    renderAt('abc123')
    const unavailableHeading = await screen.findByRole('heading', {
      name: /isn't active right now/i,
    })
    expect((unavailableHeading.parentElement as HTMLElement).className).toContain(
      BOXED_SIGNATURE_CLASS,
    )
    // By design (DESIGN_PROPOSAL.md §5), unavailable has no retry button, so
    // the only user-triggerable boxed->plain transition is error->ready,
    // covered in the next test.
    expect(screen.queryByRole('button', { name: /try again/i })).toBeNull()
  })

  it('switching from error to ready on retry swaps the boxed Card for the plain full-bleed Card with no stale boxed styling', async () => {
    vi.mocked(fetch).mockRejectedValueOnce(new TypeError('network error'))
    renderAt('abc123')
    await waitFor(() => screen.getByText(/we hit a snag/i))

    vi.mocked(fetch).mockReturnValue(
      jsonResponse(200, {
        publicId: 'abc123',
        title: 'Recovered guide',
        description: 'Recovered body.',
        isActive: true,
        createdAt: '2026-06-01',
      }),
    )
    screen.getByRole('button', { name: /try again/i }).click()

    const heading = await screen.findByRole('heading', { name: /recovered guide/i })
    const card = heading.parentElement as HTMLElement
    expect(card.className).not.toContain(BOXED_SIGNATURE_CLASS)
    expect(screen.queryByText(/we hit a snag/i)).toBeNull()
    expect(screen.queryByRole('button', { name: /try again/i })).toBeNull()
  })
})
