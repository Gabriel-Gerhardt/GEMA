import { Link } from 'react-router-dom'
import Button from '../components/Button'
import Card from '../components/Card'
import { SunflowerMark } from '../components/Logo'

const PRODUCT_STEPS: { title: string; body: string }[] = [
  {
    title: '1. Crie seu código',
    body: 'Adicione um guia curto com o que alguém precisa saber ou fazer em uma emergência.',
  },
  {
    title: '2. Leve com você',
    body: 'Imprima, use ou prenda onde for mais útil — no crachá, na mochila, na pulseira.',
  },
  {
    title: '3. Alguém escaneia, e ajuda',
    body: 'Qualquer pessoa que escanear o código vê o guia na hora, sem precisar de conta.',
  },
]

export default function LandingPage() {
  return (
    <main className="mx-auto max-w-5xl px-4 py-8">
      <section className="mx-auto flex max-w-3xl flex-col items-center px-4 py-16 text-center sm:py-20">
        <SunflowerMark size={64} />
        <h1 className="mt-4 text-[32px] font-bold text-text-warm-900">
          Um QR code que pode ajudar em um momento de crise.
        </h1>
        <p className="mt-2 text-base text-text-warm-600">
          GEMA cria códigos QR pessoais com as informações que alguém precisa
          para apoiar você — ou a pessoa que você cuida — em uma emergência.
        </p>

        <div className="mt-6 flex flex-col items-center gap-3">
          <Link to="/home">
            <Button variant="primary">Começar</Button>
          </Link>
          <Link to="/login" className="text-sm font-medium text-leaf-green underline">
            Já tem uma conta? Entrar
          </Link>
        </div>
      </section>

      <section className="mx-auto max-w-2xl">
        <Card>
          <h2 className="text-2xl font-bold text-text-warm-900">Quem somos</h2>
          <p className="mt-2 text-base text-text-warm-600">
            Nossa marca combina o cordão de girassol — um símbolo usado para
            indicar uma deficiência oculta ou a necessidade de apoio — com o
            verde da tradição dos cordões de conscientização. Pétalas verdes,
            um centro dourado e acolhedor: um sinal discreto de que ajuda pode
            ser pedida e oferecida.
          </p>
          <p className="mt-2 text-base text-text-warm-600">
            A GEMA existe para ajudar pessoas autistas e pessoas com outras
            condições a se sentirem mais seguras, e para ajudar quem está por
            perto a saber como apoiar — no momento exato em que isso importa.
          </p>
        </Card>
      </section>

      <section className="mt-10">
        <h2 className="text-2xl font-bold text-text-warm-900">Como funciona</h2>
        <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-3">
          {PRODUCT_STEPS.map((step) => (
            <Card key={step.title}>
              <h3 className="text-lg font-semibold text-text-warm-900">
                {step.title}
              </h3>
              <p className="mt-1 text-sm text-text-warm-600">{step.body}</p>
            </Card>
          ))}
        </div>
      </section>

      <div className="mt-12 flex justify-center">
        <Link to="/home">
          <Button variant="primary">Começar</Button>
        </Link>
      </div>
    </main>
  )
}
