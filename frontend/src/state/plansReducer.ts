import type { Plan, Section } from './types';

type NewSection = Pick<Section, 'title' | 'content'> & Partial<Pick<Section, 'id'>>;

export type PlansAction =
  | { type: 'ADD_PLAN'; plan: Plan }
  | { type: 'UPDATE_PLAN'; id: string; title: string; sections: NewSection[] }
  | { type: 'DELETE_PLAN'; id: string }
  | { type: 'TOGGLE_ACTIVE'; id: string };

let idCounter = 0;
/** Sequential + random suffix: unique enough for local mock state, no
 * backend to coordinate with. */
function nextId(prefix: string): string {
  idCounter += 1;
  return `${prefix}-${idCounter}-${Math.random().toString(36).slice(2, 8)}`;
}

function withIds(sections: NewSection[]): Section[] {
  return sections.map((s) => ({ id: s.id ?? nextId('section'), title: s.title, content: s.content }));
}

/** Builds a full `Plan` (ids, public id, created-at) from just a title and
 * sections — used by callers that need the created plan's id back
 * synchronously (e.g. to navigate to it), since dispatched actions don't
 * return a value. */
export function buildPlan(title: string, sections: NewSection[]): Plan {
  return {
    id: nextId('plan'),
    publicId: nextId('pub').replace('pub-', ''),
    title,
    active: true,
    createdAt: new Date().toISOString().slice(0, 10),
    sections: withIds(sections),
  };
}

export function plansReducer(plans: Plan[], action: PlansAction): Plan[] {
  switch (action.type) {
    case 'ADD_PLAN':
      return [action.plan, ...plans];
    case 'UPDATE_PLAN':
      return plans.map((p) =>
        p.id === action.id ? { ...p, title: action.title, sections: withIds(action.sections) } : p,
      );
    case 'DELETE_PLAN':
      return plans.filter((p) => p.id !== action.id);
    case 'TOGGLE_ACTIVE':
      return plans.map((p) => (p.id === action.id ? { ...p, active: !p.active } : p));
    default:
      return plans;
  }
}
