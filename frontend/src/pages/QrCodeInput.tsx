import Button from '../components/Button'
import Card from '../components/Card'

export default function QrCodeInput() {
  return (
    <main className="mx-auto max-w-md px-4 py-8">
      <p className="mb-4 inline-block rounded-md bg-primary-yellow px-3 py-1 text-sm font-medium text-text-gray-900">
        Wireframe — no backend wired up
      </p>
      <h1 className="text-[32px] font-bold text-text-gray-900">Scan a QR code</h1>

      <Card className="mt-4 flex flex-col items-center gap-4 text-center">
        <div className="flex h-48 w-48 items-center justify-center rounded-md border-2 border-dashed border-border-gray-200 text-sm text-text-gray-600">
          Camera preview placeholder
        </div>
        <p className="text-base text-text-gray-600">
          Point your camera at a QR code to scan it.
        </p>
        <Button variant="primary">Start scanning</Button>
      </Card>
    </main>
  )
}
