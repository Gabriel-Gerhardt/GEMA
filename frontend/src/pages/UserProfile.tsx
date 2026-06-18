import Button from '../components/Button'
import Card from '../components/Card'

const MOCK_USER = {
  name: 'Jane Doe',
  email: 'jane.doe@example.com',
  qrCodesCreated: 12,
}

export default function UserProfile() {
  return (
    <main className="mx-auto max-w-2xl px-4 py-8">
      <p className="mb-4 inline-block rounded-md bg-mint-50 px-3 py-1 text-sm font-medium text-leaf-green">
        Wireframe — no backend wired up
      </p>

      <Card>
        <h1 className="text-2xl font-bold text-text-warm-900">{MOCK_USER.name}</h1>
        <p className="mt-1 text-base text-text-warm-600">{MOCK_USER.email}</p>

        <div className="mt-6 border-t border-border-warm-200 pt-4">
          <p className="text-sm font-medium text-text-warm-600">QR codes created</p>
          <p className="text-2xl font-bold text-text-warm-900">
            {MOCK_USER.qrCodesCreated}
          </p>
        </div>

        <div className="mt-6 flex items-center justify-between border-t border-border-warm-200 pt-4">
          <Button variant="secondary">Edit profile</Button>
          <Button variant="danger" className="px-3 py-1.5 text-sm">
            Delete account
          </Button>
        </div>
      </Card>
    </main>
  )
}
