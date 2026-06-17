import type { HTMLAttributes } from 'react'

export type CardProps = HTMLAttributes<HTMLDivElement>

export default function Card({ className = '', children, ...props }: CardProps) {
  return (
    <div
      className={`rounded-lg border border-border-gray-200 bg-base-white p-4 shadow-sm ${className}`}
      {...props}
    >
      {children}
    </div>
  )
}
