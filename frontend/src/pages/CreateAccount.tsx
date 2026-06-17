import { Link } from 'react-router-dom'
import Button from '../components/Button'
import Card from '../components/Card'
import Input from '../components/Input'

export default function CreateAccount() {
  return (
    <main className="mx-auto flex max-w-md flex-col px-4 py-8">
      <p className="mb-4 inline-block rounded-md bg-primary-yellow px-3 py-1 text-sm font-medium text-text-gray-900">
        Wireframe — no backend wired up
      </p>
      <Card>
        <h1 className="text-2xl font-bold text-text-gray-900">
          Create your account
        </h1>
        <form className="mt-4 flex flex-col gap-4">
          <Input label="Name" placeholder="Jane Doe" />
          <Input label="Email" type="email" placeholder="you@example.com" />
          <Input
            label="Password"
            type="password"
            helperText="At least 8 characters."
          />
          <Button variant="primary" type="submit">
            Create account
          </Button>
        </form>
        <p className="mt-4 text-sm text-text-gray-600">
          Already have an account?{' '}
          <Link to="/login" className="font-medium text-text-gray-900 underline">
            Log in
          </Link>
        </p>
      </Card>
    </main>
  )
}
