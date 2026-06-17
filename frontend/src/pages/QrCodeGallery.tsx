import { Link } from 'react-router-dom'
import Card from '../components/Card'

const MOCK_CODES = [
  { id: '1', publicId: 'abc123', label: 'Wi-Fi guest network', createdAt: '2026-06-01' },
  { id: '2', publicId: 'def456', label: 'Event check-in', createdAt: '2026-06-05' },
  { id: '3', publicId: 'ghi789', label: 'Product packaging', createdAt: '2026-06-12' },
]

export default function QrCodeGallery() {
  return (
    <main className="mx-auto max-w-3xl px-4 py-8">
      <p className="mb-4 inline-block rounded-md bg-mint-50 px-3 py-1 text-sm font-medium text-leaf-green">
        Wireframe — no backend wired up
      </p>
      <h1 className="text-[32px] font-bold text-text-warm-900">QR code gallery</h1>

      <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
        {MOCK_CODES.map((code) => (
          <Link key={code.id} to={`/qr/${code.publicId}/edit`} className="block">
            <Card className="transition-shadow hover:shadow-md">
              <div className="mb-3 h-32 w-full rounded-md bg-mint-50" />
              <h3 className="text-lg font-semibold text-text-warm-900">
                {code.label}
              </h3>
              <p className="text-sm text-text-warm-600">Created {code.createdAt}</p>
            </Card>
          </Link>
        ))}
      </div>
    </main>
  )
}
