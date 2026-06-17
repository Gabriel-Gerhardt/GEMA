import Button from '../components/Button'
import Card from '../components/Card'
import Input from '../components/Input'

const COLORS = [
  { name: 'primary/yellow', token: 'bg-primary-yellow', hex: '#FFC81C' },
  { name: 'primary/yellow-dark', token: 'bg-primary-yellow-dark', hex: '#E0A800' },
  { name: 'base/white', token: 'bg-base-white', hex: '#FFFFFF' },
  { name: 'surface/gray-50', token: 'bg-surface-gray-50', hex: '#FAFAF9' },
  { name: 'border/gray-200', token: 'bg-border-gray-200', hex: '#E5E5E2' },
  { name: 'text/gray-900', token: 'bg-text-gray-900', hex: '#262624' },
  { name: 'text/gray-600', token: 'bg-text-gray-600', hex: '#5C5C58' },
  { name: 'semantic/success', token: 'bg-semantic-success', hex: '#2F9E4F' },
  { name: 'semantic/danger', token: 'bg-semantic-danger', hex: '#D14040' },
]

const TYPE_SCALE = [
  { name: 'H1', className: 'text-[32px] font-bold', spec: '32 / Bold' },
  { name: 'H2', className: 'text-2xl font-bold', spec: '24 / Bold' },
  { name: 'H3', className: 'text-lg font-semibold', spec: '18 / SemiBold' },
  { name: 'Body', className: 'text-base font-normal', spec: '16 / Regular' },
  { name: 'Caption', className: 'text-[13px] font-normal', spec: '13 / Regular' },
]

export default function StyleGuide() {
  return (
    <div>
      <main className="mx-auto max-w-5xl px-4 py-8">
        <p className="mb-4 inline-block rounded-md bg-primary-yellow px-3 py-1 text-sm font-medium text-text-gray-900">
          Wireframe — no backend wired up
        </p>
        <h1 className="text-[32px] font-bold text-text-gray-900">Style guide</h1>
        <p className="mt-2 text-base text-text-gray-600">
          Living reference for the GEMA design system. See DESIGN.md at the repo
          root for full documentation.
        </p>

        <section className="mt-10">
          <h2 className="text-2xl font-bold text-text-gray-900">Colors</h2>
          <div className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-3">
            {COLORS.map((color) => (
              <div key={color.name}>
                <div
                  className={`h-16 w-full rounded-md border border-border-gray-200 ${color.token}`}
                />
                <p className="mt-2 text-sm font-medium text-text-gray-900">
                  {color.name}
                </p>
                <p className="text-sm text-text-gray-600">{color.hex}</p>
              </div>
            ))}
          </div>
        </section>

        <section className="mt-10">
          <h2 className="text-2xl font-bold text-text-gray-900">Typography</h2>
          <div className="mt-4 flex flex-col gap-3">
            {TYPE_SCALE.map((type) => (
              <div key={type.name} className="flex items-baseline gap-4">
                <span className={type.className}>{type.name} — The quick fox</span>
                <span className="text-sm text-text-gray-600">{type.spec}</span>
              </div>
            ))}
          </div>
        </section>

        <section className="mt-10">
          <h2 className="text-2xl font-bold text-text-gray-900">Buttons</h2>
          <div className="mt-4 flex flex-wrap gap-3">
            <Button variant="primary">Primary</Button>
            <Button variant="secondary">Secondary</Button>
            <Button variant="danger">Danger</Button>
            <Button variant="primary" disabled>
              Disabled
            </Button>
          </div>
        </section>

        <section className="mt-10">
          <h2 className="text-2xl font-bold text-text-gray-900">Inputs</h2>
          <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Input label="Default" placeholder="Type here" helperText="Helper text" />
            <Input
              label="With error"
              placeholder="Type here"
              errorText="This field is required"
            />
          </div>
        </section>

        <section className="mt-10">
          <h2 className="text-2xl font-bold text-text-gray-900">Card</h2>
          <Card className="mt-4">
            <p className="text-base text-text-gray-900">
              Cards are simple bordered, padded containers with a subtle shadow.
            </p>
          </Card>
        </section>

        <section className="mt-10">
          <h2 className="text-2xl font-bold text-text-gray-900">Header</h2>
          <p className="mt-2 text-base text-text-gray-600">
            The site header is rendered above on every page (brand + Home /
            Gallery / Profile nav links).
          </p>
        </section>
      </main>
    </div>
  )
}
