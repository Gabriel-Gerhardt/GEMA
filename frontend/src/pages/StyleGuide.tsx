import Button from '../components/Button'
import Card from '../components/Card'
import Header from '../components/Header'
import Input from '../components/Input'
import { SunflowerMark, SunflowerWordmark } from '../components/Logo'

const COLORS = [
  { name: 'primary/green', token: 'bg-primary-green', hex: '#4F8F3D' },
  { name: 'primary/green-dark', token: 'bg-primary-green-dark', hex: '#3E7A2F' },
  { name: 'leaf/green', token: 'bg-leaf-green', hex: '#2F5233' },
  { name: 'mint/50', token: 'bg-mint-50', hex: '#E9F3E3' },
  { name: 'accent/gold', token: 'bg-accent-gold', hex: '#C97B2E' },
  { name: 'accent/gold-dark', token: 'bg-accent-gold-dark', hex: '#8F5219' },
  { name: 'base/white', token: 'bg-base-white', hex: '#FFFFFF' },
  { name: 'surface/cream', token: 'bg-surface-cream', hex: '#FBF8F0' },
  { name: 'border/warm-200', token: 'bg-border-warm-200', hex: '#E8E1D3' },
  { name: 'text/warm-900', token: 'bg-text-warm-900', hex: '#332E22' },
  { name: 'text/warm-600', token: 'bg-text-warm-600', hex: '#6B6354' },
  { name: 'semantic/success', token: 'bg-semantic-success', hex: '#27803F' },
  { name: 'semantic/danger', token: 'bg-semantic-danger', hex: '#C23030' },
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
        <p className="mb-4 inline-block rounded-md bg-mint-50 px-3 py-1 text-sm font-medium text-leaf-green">
          Wireframe — no backend wired up
        </p>
        <h1 className="text-[32px] font-bold text-text-warm-900">Style guide</h1>
        <p className="mt-2 text-base text-text-warm-600">
          Living reference for the GEMA design system. See DESIGN.md at the repo
          root for full documentation.
        </p>

        <section className="mt-10">
          <h2 className="text-2xl font-bold text-text-warm-900">Colors</h2>
          <div className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-3">
            {COLORS.map((color) => (
              <div key={color.name}>
                <div
                  className={`h-16 w-full rounded-md border border-border-warm-200 ${color.token}`}
                />
                <p className="mt-2 text-sm font-medium text-text-warm-900">
                  {color.name}
                </p>
                <p className="text-sm text-text-warm-600">{color.hex}</p>
              </div>
            ))}
          </div>
        </section>

        <section className="mt-10">
          <h2 className="text-2xl font-bold text-text-warm-900">Typography</h2>
          <div className="mt-4 flex flex-col gap-3">
            {TYPE_SCALE.map((type) => (
              <div key={type.name} className="flex items-baseline gap-4">
                <span className={type.className}>{type.name} — The quick fox</span>
                <span className="text-sm text-text-warm-600">{type.spec}</span>
              </div>
            ))}
          </div>
        </section>

        <section className="mt-10">
          <h2 className="text-2xl font-bold text-text-warm-900">Buttons</h2>
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
          <h2 className="text-2xl font-bold text-text-warm-900">Inputs</h2>
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
          <h2 className="text-2xl font-bold text-text-warm-900">Card</h2>
          <p className="mt-2 text-base text-text-warm-600">
            <code>variant="boxed"</code> (default) is the bordered, padded
            panel. <code>variant="plain"</code> drops the border/shadow/
            padding for full-bleed content, used by the active Emergency
            Guide View.
          </p>
          <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Card>
              <p className="text-base text-text-warm-900">
                variant="boxed" — bordered, padded, subtle shadow.
              </p>
            </Card>
            <Card variant="plain" className="border border-dashed border-border-warm-200 p-4">
              <p className="text-base text-text-warm-900">
                variant="plain" — no border/shadow/background of its own
                (dashed outline added here only to show its bounds).
              </p>
            </Card>
          </div>
        </section>

        <section className="mt-10">
          <h2 className="text-2xl font-bold text-text-warm-900">Logo</h2>
          <p className="mt-2 text-base text-text-warm-600">
            Use <code>SunflowerMark</code> alone for icon-only contexts
            (favicons, public layout header); use <code>SunflowerWordmark</code>{' '}
            (mark + "GEMA" text) wherever the full brand lockup fits, like the
            app header.
          </p>
          <div className="mt-4 flex flex-wrap items-center gap-8">
            <SunflowerMark size={48} />
            <SunflowerWordmark size={36} />
          </div>
        </section>

        <section className="mt-10">
          <h2 className="text-2xl font-bold text-text-warm-900">Header</h2>
          <p className="mt-2 text-base text-text-warm-600">
            The site header is rendered above on every page (brand + Home /
            Gallery / Profile nav links). Passing a <code>progress</code>{' '}
            prop (<code>{'{ step, total }'}</code>) swaps the nav for a step
            indicator, used by the Onboarding flow.
          </p>
          <div className="mt-4 overflow-hidden rounded-lg border border-border-warm-200">
            <Header progress={{ step: 2, total: 3 }} />
          </div>
        </section>
      </main>
    </div>
  )
}
