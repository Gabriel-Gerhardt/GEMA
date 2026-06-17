import { useRef, useState, type FormEvent } from 'react'
import Button from '../components/Button'
import Card from '../components/Card'
import ErrorState from '../components/ErrorState'
import Input from '../components/Input'
import { ApiError, apiClient } from '../lib/apiClient'

interface QrcodeCreateResponse {
  publicId: string
}

interface FieldErrors {
  title?: string
  description?: string
  userId?: string
}

type FormState =
  | { status: 'idle' | 'submitting' }
  | { status: 'error'; message?: string }
  | { status: 'success'; publicId: string }

function fieldErrorFromMessage(message: string): FieldErrors {
  // Backend validation errors come back as "<field>: <reason>" (see
  // GlobalExceptionHandler#handleValidation) or a plain message for
  // business-rule failures like "User not found".
  const [field, ...rest] = message.split(':')
  if (rest.length > 0 && ['title', 'description', 'userId'].includes(field)) {
    return { [field]: rest.join(':').trim() } as FieldErrors
  }
  if (/user/i.test(message)) {
    return { userId: message }
  }
  return {}
}

export default function QrCodeInput() {
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [userId, setUserId] = useState('')
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [form, setForm] = useState<FormState>({ status: 'idle' })
  const submittingRef = useRef(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    if (submittingRef.current) return
    submittingRef.current = true
    setFieldErrors({})
    setForm({ status: 'submitting' })
    try {
      const result = await apiClient.post<QrcodeCreateResponse>('/api/qrcodes', {
        title,
        description,
        userId: Number(userId),
      })
      setForm({ status: 'success', publicId: result.publicId })
    } catch (err) {
      if (err instanceof ApiError && err.status === 400) {
        const errors = fieldErrorFromMessage(err.message)
        if (Object.keys(errors).length === 0) {
          setForm({ status: 'error', message: err.message })
          return
        }
        setFieldErrors(errors)
        setForm({ status: 'idle' })
        return
      }
      setForm({ status: 'error' })
    } finally {
      submittingRef.current = false
    }
  }

  if (form.status === 'success') {
    const shareUrl = `${window.location.origin}/q/${form.publicId}`
    return (
      <main className="mx-auto max-w-md px-4 py-8">
        <Card className="text-center">
          <h1 className="text-2xl font-bold text-text-warm-900">QR code created</h1>
          <p className="mt-3 text-base text-text-warm-600">
            Share this link, or generate a QR code from it:
          </p>
          <p className="mt-4 break-all rounded-md bg-mint-50 px-3 py-2 text-sm font-medium text-leaf-green">
            {shareUrl}
          </p>
        </Card>
      </main>
    )
  }

  return (
    <main className="mx-auto max-w-md px-4 py-8">
      <h1 className="text-[32px] font-bold text-text-warm-900">Create a QR code</h1>

      {form.status === 'error' ? (
        <ErrorState
          message={
            form.message ??
            "We couldn't create your QR code. Please check your connection and try again."
          }
          onRetry={() => setForm({ status: 'idle' })}
        />
      ) : (
        <Card className="mt-4">
          <form className="flex flex-col gap-4" onSubmit={submit}>
            <Input
              label="Title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              errorText={fieldErrors.title}
              required
            />
            <Input
              label="Description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              errorText={fieldErrors.description}
              required
            />
            <Input
              label="User ID"
              helperText="Your existing user ID."
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              errorText={fieldErrors.userId}
              inputMode="numeric"
              required
            />
            <Button
              type="submit"
              variant="primary"
              disabled={form.status === 'submitting'}
            >
              {form.status === 'submitting' ? 'Creating…' : 'Create QR code'}
            </Button>
          </form>
        </Card>
      )}
    </main>
  )
}
