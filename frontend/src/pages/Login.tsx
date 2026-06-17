import { Link } from 'react-router-dom'
import Button from '../components/Button'
import Card from '../components/Card'
import Input from '../components/Input'

export default function Login() {
  return (
    <main className="mx-auto flex max-w-md flex-col px-4 py-8">
      <p className="mb-4 inline-block rounded-md bg-primary-yellow px-3 py-1 text-sm font-medium text-text-gray-900">
        Wireframe — no backend wired up
      </p>
      <Card>
        <h1 className="text-2xl font-bold text-text-gray-900">Log in</h1>
        <form className="mt-4 flex flex-col gap-4">
          <Input label="Email" type="email" placeholder="you@example.com" />
          <Input label="Password" type="password" placeholder="********" />
          <Button variant="primary" type="submit">
            Log in
          </Button>
        </form>
        <p className="mt-4 text-sm text-text-gray-600">
          Don&apos;t have an account?{' '}
          <Link to="/create-account" className="font-medium text-text-gray-900 underline">
            Create one
          </Link>
        </p>
      </Card>
    </main>
  )
}
