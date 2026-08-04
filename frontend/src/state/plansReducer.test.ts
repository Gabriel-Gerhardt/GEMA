import { buildPlan, plansReducer } from './plansReducer';
import type { Plan } from './types';

const existingPlan: Plan = {
  id: 'p1',
  publicId: 'abc123',
  title: 'Guia do Lucas',
  active: true,
  createdAt: '2026-06-01',
  sections: [{ id: 's1', title: 'Sobre mim', content: 'Sou autista.' }],
};

describe('buildPlan', () => {
  it('builds a plan with generated ids, an active flag, and today\'s date', () => {
    const plan = buildPlan('Guia da Ana', [{ title: 'Sobre mim', content: 'Texto' }]);
    expect(plan.title).toBe('Guia da Ana');
    expect(plan.active).toBe(true);
    expect(plan.id).toBeTruthy();
    expect(plan.publicId).toBeTruthy();
    expect(plan.sections).toEqual([expect.objectContaining({ title: 'Sobre mim', content: 'Texto' })]);
    expect(plan.sections[0].id).toBeTruthy();
  });

  it('generates distinct ids across calls', () => {
    const a = buildPlan('A', []);
    const b = buildPlan('B', []);
    expect(a.id).not.toBe(b.id);
    expect(a.publicId).not.toBe(b.publicId);
  });
});

describe('plansReducer', () => {
  it('ADD_PLAN prepends the given plan to the list', () => {
    const newPlan = buildPlan('Guia da Ana', []);
    const result = plansReducer([existingPlan], { type: 'ADD_PLAN', plan: newPlan });
    expect(result).toEqual([newPlan, existingPlan]);
  });

  it('UPDATE_PLAN updates an existing plan\'s title and sections without touching others', () => {
    const other: Plan = { ...existingPlan, id: 'p2', title: 'Outro' };
    const result = plansReducer([existingPlan, other], {
      type: 'UPDATE_PLAN',
      id: 'p1',
      title: 'Guia do Lucas (editado)',
      sections: [{ id: 's1', title: 'Sobre mim', content: 'Texto novo' }],
    });
    expect(result.find((p) => p.id === 'p1')?.title).toBe('Guia do Lucas (editado)');
    expect(result.find((p) => p.id === 'p1')?.sections[0].content).toBe('Texto novo');
    expect(result.find((p) => p.id === 'p2')).toEqual(other);
  });

  it('DELETE_PLAN removes a plan by id', () => {
    const result = plansReducer([existingPlan], { type: 'DELETE_PLAN', id: 'p1' });
    expect(result).toHaveLength(0);
  });

  it('TOGGLE_ACTIVE flips a plan\'s active flag', () => {
    const result = plansReducer([existingPlan], { type: 'TOGGLE_ACTIVE', id: 'p1' });
    expect(result[0].active).toBe(false);
    const toggledBack = plansReducer(result, { type: 'TOGGLE_ACTIVE', id: 'p1' });
    expect(toggledBack[0].active).toBe(true);
  });

  it('returns the same list for an unknown plan id', () => {
    const result = plansReducer([existingPlan], { type: 'TOGGLE_ACTIVE', id: 'does-not-exist' });
    expect(result).toEqual([existingPlan]);
  });
});
