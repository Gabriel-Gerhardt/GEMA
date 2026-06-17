import Button from '../components/Button'
import Card from '../components/Card'

export default function QrCodeInput() {
  return (
    <main className="mx-auto max-w-md px-4 py-8">
      <p className="mb-4 inline-block rounded-md bg-mint-50 px-3 py-1 text-sm font-medium text-leaf-green">
        Wireframe — no backend wired up
      </p>
      <h1 className="text-[32px] font-bold text-text-warm-900">Scan a QR code</h1>

      <Card className="mt-4 flex flex-col items-center gap-4 text-center">
        <div className="flex h-48 w-48 items-center justify-center rounded-md border-2 border-dashed border-border-warm-200 text-sm text-text-warm-600">
          Camera preview placeholder
        </div>
        <p className="text-base text-text-warm-600">
          Point your camera at a QR code to scan it.
        </p>
        <Button variant="primary">Start scanning</Button>
      </Card>
    </main>
  )
}
