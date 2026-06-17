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
      <h1 className="text-[32px] font-bold text-text-warm-900">Profile</h1>

      <Card className="mt-4">
        <h2 className="text-2xl font-bold text-text-warm-900">{MOCK_USER.name}</h2>
        <p className="mt-1 text-base text-text-warm-600">{MOCK_USER.email}</p>
        <p className="mt-1 text-sm text-text-warm-600">
          QR codes created: {MOCK_USER.qrCodesCreated}
        </p>
        <div className="mt-4 flex gap-3">
          <Button variant="secondary">Edit profile</Button>
          <Button variant="danger">Delete account</Button>
        </div>
      </Card>
    </main>
  )
}
