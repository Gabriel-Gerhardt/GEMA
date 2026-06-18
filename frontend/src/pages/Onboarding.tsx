import { useState } from 'react'
import { Link } from 'react-router-dom'
import Button from '../components/Button'
import Card from '../components/Card'
import { SunflowerMark } from '../components/Logo'

export interface OnboardingProps {
  /** Current step (1-indexed). Defaults to internal state for standalone
   * rendering (e.g. in tests) when no parent supplies controlled state. */
  step?: number
  onStepChange?: (step: number) => void
}

export const ONBOARDING_TOTAL_STEPS = 3
const TOTAL_STEPS = ONBOARDING_TOTAL_STEPS

function clampStep(step: number) {
  return Math.min(Math.max(step, 1), TOTAL_STEPS)
}

export default function Onboarding({ step: controlledStep, onStepChange }: OnboardingProps) {
  const [internalStep, setInternalStep] = useState(1)
  const step = controlledStep ?? internalStep

  const goTo = (next: number) => {
    const clamped = clampStep(next)
    setInternalStep(clamped)
    onStepChange?.(clamped)
  }

  return (
    <main className="mx-auto max-w-md px-4 py-8">
      <p className="mb-4 inline-block rounded-md bg-mint-50 px-3 py-1 text-sm font-medium text-leaf-green">
        Wireframe — no backend wired up
      </p>

      {step === 1 ? (
        <div className="flex flex-col items-center text-center">
          <SunflowerMark size={56} />
          <h1 className="mt-3 text-[32px] font-bold text-text-warm-900">
            Welcome to GEMA
          </h1>
          <p className="mt-2 text-base text-text-warm-600">
            Our mark combines the sunflower lanyard — a symbol worn to signal a
            hidden disability or need for support — with the green of the
            awareness-cord tradition. Green petals, a warm golden center: a
            quiet sign that help can be asked for and given.
          </p>
        </div>
      ) : step === 2 ? (
        <Card className="mt-2">
          <h2 className="text-2xl font-bold text-text-warm-900">
            1. Create a QR code
          </h2>
          <p className="mt-2 text-base text-text-warm-600">
            Add a short guide describing what someone should know or do if
            they scan your code.
          </p>
        </Card>
      ) : (
        <Card className="mt-2">
          <h2 className="text-2xl font-bold text-text-warm-900">2. Share it</h2>
          <p className="mt-2 text-base text-text-warm-600">
            Print it, wear it, or attach it wherever it's needed. Anyone who
            scans it sees your guide instantly — no account required.
          </p>
        </Card>
      )}

      <div className="mt-8 flex items-center justify-between">
        {step > 1 ? (
          <Button variant="secondary" onClick={() => goTo(step - 1)}>
            Back
          </Button>
        ) : (
          <span />
        )}

        {step < TOTAL_STEPS ? (
          <Button variant="primary" onClick={() => goTo(step + 1)}>
            Next
          </Button>
        ) : (
          <Link to="/create-account">
            <Button variant="primary">Get started</Button>
          </Link>
        )}
      </div>
    </main>
  )
}
