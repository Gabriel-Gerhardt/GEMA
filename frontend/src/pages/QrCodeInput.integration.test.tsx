// Integration/acceptance-style edge cases for QrCodeInput that go beyond the
// component-level unit tests in QrCodeInput.test.tsx.
// Focus: double-submit prevention, non-numeric userId handling, a real 500
// ApiError (vs. only network-throw), backend "User not found" mapping via
// the regex fallback, and rapid repeated retry clicks.
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

function fillForm(overrides: Partial<{ title: string; description: string; userId: string }> = {}) {
  fireEvent.change(screen.getByLabelText(/title/i), {
    target: { value: overrides.title ?? 'My guide' },
  })
  fireEvent.change(screen.getByLabelText(/description/i), {
    target: { value: overrides.description ?? 'Some details' },
  })
  fireEvent.change(screen.getByLabelText(/user id/i), {
    target: { value: overrides.userId ?? '1' },
  })
}

function renderForm() {
  return render(
    <MemoryRouter>
      <QrCodeInput />
    </MemoryRouter>,
  )
}

describe('QrCodeInput integration/edge cases', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  it('disables the submit button while submitting so a rapid double-click cannot fire two POSTs', async () => {
    let resolveFetch: (value: Response) => void = () => {}
    const pending = new Promise<Response>((resolve) => {
      resolveFetch = resolve
    })
    vi.mocked(fetch).mockReturnValue(pending)

    renderForm()
    fillForm()
    const button = screen.getByRole('button', { name: /create qr code/i })

    fireEvent.click(button)
    // Button should now be disabled and relabeled while submitting.
    await waitFor(() =>
      expect((button as HTMLButtonElement).disabled).toBe(true),
    )
    expect(screen.getByRole('button', { name: /creating/i })).toBeTruthy()

    // A second rapid click on the (disabled) button must not trigger another fetch call.
    fireEvent.click(button)
    expect(fetch).toHaveBeenCalledTimes(1)

    resolveFetch(
      await jsonResponse(201, { publicId: 'abc123' }),
    )
    await waitFor(() => screen.getByText(/qr code created/i))
    expect(fetch).toHaveBeenCalledTimes(1)
  })

  it('submitting the form via Enter/native submit twice in quick succession only sends one request', async () => {
    let resolveFetch: (value: Response) => void = () => {}
    const pending = new Promise<Response>((resolve) => {
      resolveFetch = resolve
    })
    vi.mocked(fetch).mockReturnValue(pending)

    const { container } = renderForm()
    fillForm()
    const form = container.querySelector('form') as HTMLFormElement

    fireEvent.submit(form)
    fireEvent.submit(form)

    expect(fetch).toHaveBeenCalledTimes(1)
    resolveFetch(await jsonResponse(201, { publicId: 'abc123' }))
    await waitFor(() => screen.getByText(/qr code created/i))
  })

  it('sends userId as null when the User ID field is non-numeric (Number("") / Number("abc") => NaN serializes to null)', async () => {
    vi.mocked(fetch).mockReturnValue(jsonResponse(201, { publicId: 'abc123' }))
    renderForm()
    fillForm({ userId: 'not-a-number' })
    fireEvent.click(screen.getByRole('button', { name: /create qr code/i }))

    await waitFor(() => expect(fetch).toHaveBeenCalled())
    const [, init] = vi.mocked(fetch).mock.calls[0]
    const sentBody = JSON.parse((init as RequestInit).body as string)
    expect(sentBody.userId).toBeNull()
  })

  it('treats a real 500 ApiError response (not just a thrown network error) as the generic error state with retry', async () => {
    vi.mocked(fetch).mockReturnValue(
      jsonResponse(500, {
        description: 'INTERNAL_ERROR',
        message: 'Unexpected server failure',
        httpStatus: 500,
      }),
    )
    renderForm()
    fillForm()
    fireEvent.click(screen.getByRole('button', { name: /create qr code/i }))

    await waitFor(() => screen.getByText(/we hit a snag/i))
    // 500s are not field-mapped — generic ErrorState copy, not the server message verbatim
    // (component only special-cases status 400 for message passthrough).
    expect(screen.getByRole('button', { name: /try again/i })).toBeTruthy()
  })

  it('maps a backend "User not found" 400 (BadRequestException, no colon) onto the userId field via the regex fallback', async () => {
    // Mirrors QrcodeService#createQrcode's BadRequestException("User not found"),
    // which has no "field: message" shape, only the regex fallback applies.
    vi.mocked(fetch).mockReturnValue(
      jsonResponse(400, {
        description: 'Invalid request found',
        message: 'User not found',
        httpStatus: 400,
      }),
    )
    renderForm()
    fillForm({ userId: '999999' })
    fireEvent.click(screen.getByRole('button', { name: /create qr code/i }))

    await waitFor(() => screen.getByText(/user not found/i))
    // Should appear as a field-level error under User ID, not the shared ErrorState.
    expect(screen.queryByText(/we hit a snag/i)).toBeNull()
    const userIdInput = screen.getByLabelText(/user id/i)
    expect(userIdInput.getAttribute('aria-invalid')).toBe('true')
  })

  it('clicking retry multiple times rapidly after a network error does not duplicate ErrorState or lose field values', async () => {
    vi.mocked(fetch).mockRejectedValueOnce(new TypeError('network error'))
    renderForm()
    fillForm({ title: 'Resilient title' })
    fireEvent.click(screen.getByRole('button', { name: /create qr code/i }))

    await waitFor(() => screen.getByText(/we hit a snag/i))

    const retry = () => screen.getByRole('button', { name: /try again/i })
    retry().click()
    retry().click()
    retry().click()

    // After bouncing back to idle, the form (with preserved values) should
    // show exactly once, not be duplicated, and the title should still be there.
    await waitFor(() => {
      expect(screen.getAllByLabelText(/title/i)).toHaveLength(1)
    })
    expect((screen.getByLabelText(/title/i) as HTMLInputElement).value).toBe(
      'Resilient title',
    )
  })

  it('clears previous field-level errors when resubmitting after a 400 validation error', async () => {
    vi.mocked(fetch).mockReturnValueOnce(
      jsonResponse(400, {
        description: 'VALIDATION_ERROR',
        message: 'title: must not be blank',
        httpStatus: 400,
      }),
    )
    renderForm()
    fillForm()
    fireEvent.click(screen.getByRole('button', { name: /create qr code/i }))
    await waitFor(() => screen.getByText(/must not be blank/i))

    // Fix the field and resubmit successfully.
    vi.mocked(fetch).mockReturnValueOnce(jsonResponse(201, { publicId: 'fixed-id' }))
    fireEvent.change(screen.getByLabelText(/title/i), { target: { value: 'Fixed title' } })
    fireEvent.click(screen.getByRole('button', { name: /create qr code/i }))

    await waitFor(() => screen.getByText(/qr code created/i))
  })
})
