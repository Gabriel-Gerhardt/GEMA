import { Link } from 'react-router-dom'
import Button from '../components/Button'
import Card from '../components/Card'

export default function Home() {
  return (
    <main className="mx-auto max-w-5xl px-4 py-8">
      <p className="mb-4 inline-block rounded-md bg-primary-yellow px-3 py-1 text-sm font-medium text-text-gray-900">
        Wireframe — no backend wired up
      </p>
      <h1 className="text-[32px] font-bold text-text-gray-900">
        Welcome to GEMA
      </h1>
      <p className="mt-2 text-base text-text-gray-600">
        Generate, scan, and manage your QR codes in one place.
      </p>

      <div className="mt-6 flex gap-3">
        <Link to="/qr/scan">
          <Button variant="primary">Scan a QR code</Button>
        </Link>
        <Link to="/qr/gallery">
          <Button variant="secondary">View gallery</Button>
        </Link>
      </div>

      <Card className="mt-8">
        <h2 className="text-2xl font-bold text-text-gray-900">
          Recent activity
        </h2>
        <p className="mt-2 text-base text-text-gray-600">
          Your recently scanned or generated codes will show up here.
        </p>
      </Card>
    </main>
  )
}
