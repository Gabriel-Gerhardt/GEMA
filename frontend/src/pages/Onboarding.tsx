import { Link } from 'react-router-dom'
import Button from '../components/Button'
import Card from '../components/Card'
import { SunflowerMark } from '../components/Logo'

export default function Onboarding() {
  return (
    <main className="mx-auto max-w-md px-4 py-8">
      <p className="mb-4 inline-block rounded-md bg-mint-50 px-3 py-1 text-sm font-medium text-leaf-green">
        Wireframe — no backend wired up
      </p>

      <div className="flex flex-col items-center text-center">
        <SunflowerMark size={56} />
        <h1 className="mt-3 text-[32px] font-bold text-text-warm-900">
          Welcome to GEMA
        </h1>
        <p className="mt-2 text-base text-text-warm-600">
          Our mark combines the sunflower lanyard — a symbol worn to signal a
          hidden disability or need for support — with the green of the
          awareness-cord tradition. Green petals, a warm golden center: a
          quiet sign that help can be asked for and given.
        </p>
      </div>

      <Card className="mt-6">
        <h2 className="text-lg font-semibold text-text-warm-900">
          1. Create a QR code
        </h2>
        <p className="mt-1 text-sm text-text-warm-600">
          Add a short guide describing what someone should know or do if they
          scan your code.
        </p>
      </Card>
      <Card className="mt-4">
        <h2 className="text-lg font-semibold text-text-warm-900">
          2. Share it
        </h2>
        <p className="mt-1 text-sm text-text-warm-600">
          Print it, wear it, or attach it wherever it's needed. Anyone who
          scans it sees your guide instantly — no account required.
        </p>
      </Card>

      <div className="mt-6 flex justify-center">
        <Link to="/create-account">
          <Button variant="primary">Get started</Button>
        </Link>
      </div>
    </main>
  )
}
