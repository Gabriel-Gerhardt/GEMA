import Button from './Button'
import Card from './Card'

export interface ErrorStateProps {
  message?: string
  onRetry: () => void
}

export default function ErrorState({
  message = "Something went wrong on our end. It's not you — please try again.",
  onRetry,
}: ErrorStateProps) {
  return (
    <Card className="text-center">
      <h1 className="text-2xl font-bold text-text-warm-900">We hit a snag</h1>
      <p className="mt-3 text-base text-text-warm-600">{message}</p>
      <Button variant="primary" className="mt-4" onClick={onRetry}>
        Try again
      </Button>
    </Card>
  )
}
