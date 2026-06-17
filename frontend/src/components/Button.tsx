import type { ButtonHTMLAttributes } from 'react'

export type ButtonVariant = 'primary' | 'secondary' | 'danger'

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
}

const VARIANT_CLASSES: Record<ButtonVariant, string> = {
  primary:
    'bg-primary-yellow text-text-gray-900 hover:bg-primary-yellow-dark',
  secondary:
    'bg-base-white text-text-gray-900 border border-border-gray-200 hover:bg-surface-gray-50',
  danger: 'bg-semantic-danger text-base-white hover:opacity-90',
}

export default function Button({
  variant = 'primary',
  disabled,
  className = '',
  ...props
}: ButtonProps) {
  return (
    <button
      type="button"
      disabled={disabled}
      className={`inline-flex items-center justify-center rounded-md px-4 py-2 text-base font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50 ${VARIANT_CLASSES[variant]} ${className}`}
      {...props}
    />
  )
}
