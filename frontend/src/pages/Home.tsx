import { Link } from 'react-router-dom'
import Button from '../components/Button'
import Card from '../components/Card'

const RECENT_ACTIVITY: { id: string; label: string; detail: string }[] = []

export default function Home() {
  return (
    <main className="mx-auto max-w-5xl px-4 py-8">
      <p className="mb-4 inline-block rounded-md bg-mint-50 px-3 py-1 text-sm font-medium text-leaf-green">
        Wireframe — no backend wired up
      </p>

      <section>
        <h1 className="text-[32px] font-bold text-text-warm-900">
          Welcome to GEMA
        </h1>
        <p className="mt-2 text-base text-text-warm-600">
          Generate, scan, and manage your QR codes in one place.
        </p>

        <div className="mt-6 flex flex-wrap gap-3">
          <Link to="/qr/scan" className="flex-1 sm:flex-none">
            <Button variant="primary" className="w-full sm:w-auto">
              Scan a QR code
            </Button>
          </Link>
          <Link to="/qr/gallery" className="flex-1 sm:flex-none">
            <Button variant="secondary" className="w-full sm:w-auto">
              View gallery
            </Button>
          </Link>
        </div>
      </section>

      <section className="mt-10">
        <h2 className="text-2xl font-bold text-text-warm-900">
          Recent activity
        </h2>
        {RECENT_ACTIVITY.length === 0 ? (
          <Card className="mt-4">
            <p className="text-base text-text-warm-600">
              Nothing here yet. Codes you scan or create will show up in this
              list.
            </p>
          </Card>
        ) : (
          <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
            {RECENT_ACTIVITY.map((item) => (
              <Card key={item.id}>
                <h3 className="text-lg font-semibold text-text-warm-900">
                  {item.label}
                </h3>
                <p className="mt-1 text-sm text-text-warm-600">{item.detail}</p>
              </Card>
            ))}
          </div>
        )}
      </section>
    </main>
  )
}
