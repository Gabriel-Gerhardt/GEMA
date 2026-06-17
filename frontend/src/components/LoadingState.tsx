export interface LoadingStateProps {
  label?: string
}

export default function LoadingState({ label = 'Loading…' }: LoadingStateProps) {
  return (
    <p className="text-base text-text-warm-600" role="status">
      {label}
    </p>
  )
}
