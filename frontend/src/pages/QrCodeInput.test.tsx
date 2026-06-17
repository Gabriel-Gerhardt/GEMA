import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import QrCodeInput from './QrCodeInput'

afterEach(cleanup)

function jsonResponse(status: number, body: unknown) {
  return Promise.resolve(
    new Response(JSON.stringify(body), {
      status,
      headers: { 'Content-Type': 'application/json' },
    }),
  )
}

function fillForm() {
  fireEvent.change(screen.getByLabelText(/title/i), { target: { value: 'My guide' } })
  fireEvent.change(screen.getByLabelText(/description/i), {
    target: { value: 'Some details' },
  })
  fireEvent.change(screen.getByLabelText(/user id/i), { target: { value: '1' } })
}

describe('QrCodeInput', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  it('shows a success state with the shareable link after creation', async () => {
    vi.mocked(fetch).mockReturnValue(jsonResponse(201, { publicId: 'abc123' }))
    render(
      <MemoryRouter>
        <QrCodeInput />
      </MemoryRouter>,
    )

    fillForm()
    fireEvent.click(screen.getByRole('button', { name: /create qr code/i }))

    await waitFor(() => screen.getByText(/qr code created/i))
    expect(screen.getByText(/\/q\/abc123/)).toBeTruthy()
  })

  it('maps a 400 validation error onto the matching field', async () => {
    vi.mocked(fetch).mockReturnValue(
      jsonResponse(400, {
        description: 'VALIDATION_ERROR',
        message: 'title: must not be blank',
        httpStatus: 400,
      }),
    )
    render(
      <MemoryRouter>
        <QrCodeInput />
      </MemoryRouter>,
    )

    fillForm()
    fireEvent.click(screen.getByRole('button', { name: /create qr code/i }))

    await waitFor(() => screen.getByText(/must not be blank/i))
    // Entered values are preserved, not cleared.
    expect((screen.getByLabelText(/description/i) as HTMLInputElement).value).toBe(
      'Some details',
    )
  })

  it('shows the shared error state for a 400 that matches no known field pattern', async () => {
    vi.mocked(fetch).mockReturnValue(
      jsonResponse(400, {
        description: 'VALIDATION_ERROR',
        message: 'Something unexpected happened',
        httpStatus: 400,
      }),
    )
    render(
      <MemoryRouter>
        <QrCodeInput />
      </MemoryRouter>,
    )

    fillForm()
    fireEvent.click(screen.getByRole('button', { name: /create qr code/i }))

    await waitFor(() => screen.getByText(/we hit a snag/i))
    expect(screen.getByText(/something unexpected happened/i)).toBeTruthy()
    expect(screen.getByRole('button', { name: /try again/i })).toBeTruthy()
  })

  it('shows the shared error state with retry on network failure, preserving field values', async () => {
    vi.mocked(fetch).mockRejectedValueOnce(new TypeError('network error'))
    render(
      <MemoryRouter>
        <QrCodeInput />
      </MemoryRouter>,
    )

    fillForm()
    fireEvent.click(screen.getByRole('button', { name: /create qr code/i }))

    await waitFor(() => screen.getByText(/we hit a snag/i))
    fireEvent.click(screen.getByRole('button', { name: /try again/i }))

    expect((screen.getByLabelText(/title/i) as HTMLInputElement).value).toBe('My guide')
  })
})
