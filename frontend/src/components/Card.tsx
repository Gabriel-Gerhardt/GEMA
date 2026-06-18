import type { HTMLAttributes } from 'react'

export type CardVariant = 'boxed' | 'plain'

export interface CardProps extends HTMLAttributes<HTMLDivElement> {
  /** 'boxed' (default) is today's bordered/shadowed panel. 'plain' drops the
   * border/shadow/background/padding for full-bleed content (e.g. the
   * crisis-read guide), keeping Card only as a semantic content wrapper. */
  variant?: CardVariant
}

const VARIANT_CLASSES: Record<CardVariant, string> = {
  boxed:
    'rounded-tl-2xl rounded-br-2xl rounded-tr-lg rounded-bl-lg border border-border-warm-200 bg-base-white p-4 shadow-sm shadow-leaf-green/5',
  plain: '',
}

export default function Card({ variant = 'boxed', className = '', children, ...props }: CardProps) {
  return (
    <div className={`${VARIANT_CLASSES[variant]} ${className}`} {...props}>
      {children}
    </div>
  )
}
