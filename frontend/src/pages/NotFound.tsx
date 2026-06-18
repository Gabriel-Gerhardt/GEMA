import { Link } from 'react-router-dom'
import Card from '../components/Card'
import { SunflowerMark } from '../components/Logo'

export default function NotFound() {
  return (
    <main className="mx-auto flex min-h-[calc(100vh-57px)] max-w-sm flex-col justify-center px-4 py-12">
      <Card className="flex flex-col items-center text-center">
        <SunflowerMark size={48} />
        <h1 className="mt-4 text-2xl font-bold text-text-warm-900">
          Page not found
        </h1>
        <p className="mt-2 text-base text-text-warm-600">
          We couldn&apos;t find what you were looking for.
        </p>
        <Link to="/" className="mt-6 font-medium text-leaf-green underline">
          Back to home
        </Link>
      </Card>
    </main>
  )
}
