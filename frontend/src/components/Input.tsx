import { useId, type InputHTMLAttributes } from 'react'

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  helperText?: string
  errorText?: string
}

export default function Input({
  label,
  helperText,
  errorText,
  id,
  className = '',
  ...props
}: InputProps) {
  const generatedId = useId()
  const inputId = id ?? generatedId
  const hasError = Boolean(errorText)

  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={inputId} className="text-sm font-medium text-text-gray-900">
        {label}
      </label>
      <input
        id={inputId}
        aria-invalid={hasError}
        className={`rounded-md border px-3 py-2 text-base text-text-gray-900 outline-none focus:ring-2 ${
          hasError
            ? 'border-semantic-danger focus:ring-semantic-danger'
            : 'border-border-gray-200 focus:ring-primary-yellow'
        } ${className}`}
        {...props}
      />
      {hasError ? (
        <span className="text-sm text-semantic-danger">{errorText}</span>
      ) : helperText ? (
        <span className="text-sm text-text-gray-600">{helperText}</span>
      ) : null}
    </div>
  )
}
