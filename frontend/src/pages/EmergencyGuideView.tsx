import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import Card from '../components/Card'

interface EmergencyGuide {
  publicId: string
  title: string
  description: string
  isActive: boolean
  createdAt: string
}

// Mock "backend" lookup — no API client exists in this frontend yet, so we
// simulate a network round trip with a timeout, exactly like the other
// wireframe pages use static mock data. Replace with a real fetch once the
// public guide endpoint exists.
const MOCK_GUIDES: Record<string, EmergencyGuide> = {
  abc123: {
    publicId: 'abc123',
    title: 'Jordan needs a little extra time and patience',
    description:
      'I have a hidden disability that can make busy or loud places overwhelming. ' +
      "I'm not in danger, but I may need a moment, a quiet space, or simple, " +
      "clear instructions. Thank you for your patience — there's no need to call " +
      'anyone unless I ask.',
    isActive: true,
    createdAt: '2026-06-01',
  },
  inactive1: {
    publicId: 'inactive1',
    title: 'Retired guide',
    description: 'This guide has been turned off by its owner.',
    isActive: false,
    createdAt: '2026-01-10',
  },
}

type ViewState =
  | { status: 'loading' }
  | { status: 'ready'; guide: EmergencyGuide }
  | { status: 'unavailable' }

export default function EmergencyGuideView() {
  const { publicId } = useParams<{ publicId: string }>()
  const [state, setState] = useState<ViewState>({ status: 'loading' })

  useEffect(() => {
    const timer = setTimeout(() => {
      const guide = publicId ? MOCK_GUIDES[publicId] : undefined
      if (guide && guide.isActive) {
        setState({ status: 'ready', guide })
      } else {
        setState({ status: 'unavailable' })
      }
    }, 600)
    return () => clearTimeout(timer)
  }, [publicId])

  if (state.status === 'loading') {
    return (
      <main className="mx-auto max-w-md px-4 py-12 text-center">
        <p className="text-base text-text-warm-600" role="status">
          Loading guide…
        </p>
      </main>
    )
  }

  if (state.status === 'unavailable') {
    return (
      <main className="mx-auto max-w-md px-4 py-12 text-center">
        <Card>
          <h1 className="text-2xl font-bold text-text-warm-900">
            This QR code isn&apos;t active right now
          </h1>
          <p className="mt-3 text-base text-text-warm-600">
            There&apos;s nothing wrong on your end — the person who made this
            code may have paused it, or it hasn&apos;t been set up yet. If
            someone nearby seems to need help, the best thing you can do is
            ask them directly and stay calm.
          </p>
        </Card>
      </main>
    )
  }

  const { guide } = state

  return (
    <main className="mx-auto max-w-md px-4 py-12">
      <Card>
        <p className="mb-3 inline-block rounded-md bg-mint-50 px-3 py-1 text-sm font-medium text-leaf-green">
          Support guide
        </p>
        <h1 className="text-2xl font-bold text-text-warm-900">{guide.title}</h1>
        <p className="mt-4 whitespace-pre-line text-base text-text-warm-900">
          {guide.description}
        </p>
      </Card>
    </main>
  )
}
