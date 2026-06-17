import type { HTMLAttributes } from 'react'

export type CardProps = HTMLAttributes<HTMLDivElement>

export default function Card({ className = '', children, ...props }: CardProps) {
  return (
    <div
      className={`rounded-tl-2xl rounded-br-2xl rounded-tr-lg rounded-bl-lg border border-border-warm-200 bg-base-white p-4 shadow-sm shadow-leaf-green/5 ${className}`}
      {...props}
    >
      {children}
    </div>
  )
}
