import type { Plan } from '../state/types';

/** Seed data for the mock PlansContext — mirrors the design handoff's own
 * example content (Guia do Lucas) plus the old wireframe's gallery variety
 * (an inactive plan), all in PT-BR per the design's final-intent copy. */
export const SEED_PLANS: Plan[] = [
  {
    id: 'seed-plan-1',
    publicId: 'abc123',
    title: 'Guia do Lucas',
    active: true,
    createdAt: '2026-06-12',
    ownerName: 'Lucas',
    emergencyContactName: 'Ana — minha mãe',
    emergencyContactPhone: '51999990000',
    sections: [
      {
        id: 'seed-section-1',
        title: 'Sobre mim',
        content:
          'Sou autista e posso ficar sobrecarregado em lugares com muito barulho ou movimento. Se me encontrou perdido ou confuso, obrigado por parar para ajudar.',
      },
      {
        id: 'seed-section-2',
        title: 'O que ajuda',
        content: 'Fale devagar e com calma. Me leve para um lugar mais silencioso, se der.',
      },
      {
        id: 'seed-section-3',
        title: 'Em uma emergência',
        content: 'Ana — minha mãe · (51) 99999-0000',
      },
    ],
  },
  {
    id: 'seed-plan-2',
    publicId: 'def456',
    title: 'Contato de emergência',
    active: true,
    createdAt: '2026-06-05',
    sections: [
      { id: 'seed-section-4', title: 'Contato', content: 'Marina — irmã · (51) 98888-0000' },
    ],
  },
  {
    id: 'seed-plan-3',
    publicId: 'ghi789',
    title: 'Rede Wi-Fi convidados',
    active: false,
    createdAt: '2026-06-01',
    sections: [{ id: 'seed-section-5', title: 'Senha', content: 'convidado2026' }],
  },
];
