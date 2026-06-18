import { useState } from 'react'
import { useParams } from 'react-router-dom'
import Button from '../components/Button'
import Card from '../components/Card'
import Input from '../components/Input'

export default function QrCodeDetail() {
  const { publicId } = useParams<{ publicId: string }>()
  const [title, setTitle] = useState('Wi-Fi guest network')
  const [description, setDescription] = useState(
    'Connect to "Guest" using the password on the card by the door.',
  )
  const [isActive, setIsActive] = useState(true)

  return (
    <main className="mx-auto max-w-2xl px-4 py-8">
      <p className="mb-4 inline-block rounded-md bg-mint-50 px-3 py-1 text-sm font-medium text-leaf-green">
        Wireframe — no backend wired up
      </p>

      <Card>
        <h1 className="text-2xl font-bold text-text-warm-900">Edit QR code</h1>
        <p className="mt-1 text-sm text-text-warm-600">Public ID: {publicId}</p>

        <form
          className="mt-6 flex flex-col gap-4 border-t border-border-warm-200 pt-4"
          onSubmit={(e) => e.preventDefault()}
        >
          <Input
            label="Title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
          />
          <div className="flex flex-col gap-1">
            <label htmlFor="description" className="text-sm font-medium text-text-warm-900">
              Description
            </label>
            <textarea
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={4}
              className="rounded-lg border border-border-warm-200 px-3 py-2 text-base text-text-warm-900 outline-none focus:ring-2 focus:ring-accent-gold"
            />
          </div>

          <label className="flex items-center gap-2 text-sm font-medium text-text-warm-900">
            <input
              type="checkbox"
              checked={isActive}
              onChange={(e) => setIsActive(e.target.checked)}
              className="h-4 w-4 accent-primary-green-dark"
            />
            Active
          </label>

          <Button variant="primary" type="submit">
            Save changes
          </Button>
        </form>

        <div className="mt-4 flex justify-end border-t border-border-warm-200 pt-4">
          <Button variant="danger" type="button" className="px-3 py-1.5 text-sm">
            Delete
          </Button>
        </div>
      </Card>
    </main>
  )
}
