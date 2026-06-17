interface SunflowerMarkProps {
  size?: number
  className?: string
}

const PETAL_COUNT = 8

/**
 * Icon-only mark: a sunflower with green petals (the awareness-cord green)
 * radiating around a warm gold-brown center — a deliberate twist on the
 * all-yellow Sunflower Lanyard symbol for hidden disabilities.
 */
export function SunflowerMark({ size = 32, className = '' }: SunflowerMarkProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 32 32"
      fill="none"
      role="img"
      aria-label="GEMA sunflower mark"
      className={className}
    >
      <g>
        {Array.from({ length: PETAL_COUNT }, (_, i) => {
          const angle = (360 / PETAL_COUNT) * i
          return (
            <ellipse
              key={i}
              cx="16"
              cy="7"
              rx="3.4"
              ry="6.4"
              fill="#4F8F3D"
              transform={`rotate(${angle} 16 16)`}
            />
          )
        })}
      </g>
      <circle cx="16" cy="16" r="5.5" fill="#C97B2E" />
    </svg>
  )
}

interface SunflowerWordmarkProps {
  size?: number
  className?: string
}

/** Horizontal lockup: mark + "GEMA" wordmark, used in the app header. */
export function SunflowerWordmark({ size = 28, className = '' }: SunflowerWordmarkProps) {
  return (
    <span className={`inline-flex items-center gap-2 ${className}`}>
      <SunflowerMark size={size} />
      <span className="text-xl font-bold text-text-warm-900">GEMA</span>
    </span>
  )
}
