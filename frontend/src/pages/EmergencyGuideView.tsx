import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import Card from '../components/Card'
import ErrorState from '../components/ErrorState'
import LoadingState from '../components/LoadingState'
import { ApiError, apiClient } from '../lib/apiClient'

interface EmergencyGuide {
  publicId: string
  title: string
  description: string
  isActive: boolean
  createdAt: string
}

type Result =
  | { status: 'ready'; guide: EmergencyGuide }
  | { status: 'unavailable' }
  | { status: 'error' }

// Each result is tagged with the request it answers. Rendering compares
// requestKey against the current (publicId, retryCount) pair, so a stale
// in-flight request can never clobber a newer one's state, and "loading"
// falls out of that comparison instead of a setState call inside the effect.
interface TaggedResult {
  requestKey: string
  result: Result
}

export default function EmergencyGuideView() {
  const { publicId } = useParams<{ publicId: string }>()
  const [retryCount, setRetryCount] = useState(0)
  const [tagged, setTagged] = useState<TaggedResult | null>(null)
  const requestKey = `${publicId ?? ''}:${retryCount}`

  useEffect(() => {
    let cancelled = false
    async function load() {
      if (!publicId) {
        if (!cancelled) setTagged({ requestKey, result: { status: 'unavailable' } })
        return
      }
      try {
        const guide = await apiClient.get<EmergencyGuide>(`/api/q/${publicId}`)
        if (cancelled) return
        setTagged({
          requestKey,
          result: guide.isActive ? { status: 'ready', guide } : { status: 'unavailable' },
        })
      } catch (err) {
        if (cancelled) return
        if (err instanceof ApiError && err.status === 404) {
          setTagged({ requestKey, result: { status: 'unavailable' } })
          return
        }
        setTagged({ requestKey, result: { status: 'error' } })
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [publicId, requestKey])

  const retry = () => setRetryCount((n) => n + 1)

  const state: Result | { status: 'loading' } =
    tagged && tagged.requestKey === requestKey ? tagged.result : { status: 'loading' }

  if (state.status === 'loading') {
    return (
      <main className="mx-auto max-w-md px-4 py-12 text-center">
        <LoadingState label="Loading guide…" />
      </main>
    )
  }

  if (state.status === 'error') {
    return (
      <main className="mx-auto max-w-md px-4 py-12 text-center">
        <ErrorState
          message="We couldn't load this guide. Please check your connection and try again."
          onRetry={retry}
        />
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
    <main className="mx-auto max-w-prose px-4 py-12">
      <Card variant="plain">
        <p className="text-sm font-medium text-leaf-green">Support guide</p>
        <h1 className="mt-2 text-[32px] font-bold text-text-warm-900">{guide.title}</h1>
        <p className="mt-6 whitespace-pre-line text-base text-text-warm-900">
          {guide.description}
        </p>
      </Card>
    </main>
  )
}
