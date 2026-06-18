import { Link } from 'react-router-dom'
import Button from '../components/Button'
import Card from '../components/Card'
import Input from '../components/Input'

export default function Login() {
  return (
    <main className="mx-auto flex min-h-[calc(100vh-57px)] max-w-sm flex-col justify-center px-4 py-8">
      <Card>
        <p className="mb-3 inline-block rounded-md bg-mint-50 px-3 py-1 text-sm font-medium text-leaf-green">
          Account
        </p>
        <h1 className="text-2xl font-bold text-text-warm-900">Log in</h1>
        <form className="mt-4 flex flex-col gap-4">
          <Input label="Email" type="email" placeholder="you@example.com" />
          <Input label="Password" type="password" placeholder="********" />
          <Button variant="primary" type="submit">
            Log in
          </Button>
        </form>
      </Card>
      <p className="mt-4 text-center text-sm text-text-warm-600">
        Don&apos;t have an account?{' '}
        <Link to="/create-account" className="font-medium text-text-warm-900 underline">
          Create one
        </Link>
      </p>
    </main>
  )
}
