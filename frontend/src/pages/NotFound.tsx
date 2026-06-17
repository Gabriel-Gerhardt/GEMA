import { Link } from 'react-router-dom'
import { SunflowerMark } from '../components/Logo'

export default function NotFound() {
  return (
    <main className="mx-auto flex max-w-md flex-col items-center px-4 py-16 text-center">
      <SunflowerMark size={48} />
      <h1 className="mt-4 text-[32px] font-bold text-text-warm-900">
        Page not found
      </h1>
      <p className="mt-2 text-base text-text-warm-600">
        We couldn&apos;t find what you were looking for.
      </p>
      <Link to="/" className="mt-6 font-medium text-leaf-green underline">
        Back to home
      </Link>
    </main>
  )
}
