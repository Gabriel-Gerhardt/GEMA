import { renderHook, act } from '@testing-library/react-native';
import type { ReactNode } from 'react';
import { PlansProvider, usePlans } from './PlansContext';

function wrapper({ children }: { children: ReactNode }) {
  return <PlansProvider>{children}</PlansProvider>;
}

describe('PlansContext', () => {
  it('seeds with the mock plans', async () => {
    const { result } = await renderHook(() => usePlans(), { wrapper });
    expect(result.current.plans.length).toBeGreaterThan(0);
  });

  it('creates, updates, toggles, and deletes a plan', async () => {
    const { result } = await renderHook(() => usePlans(), { wrapper });
    const initialCount = result.current.plans.length;

    let createdId = '';
    await act(async () => {
      const created = result.current.createPlan('Guia de teste', [{ title: 'T', content: 'C' }]);
      createdId = created.id;
    });
    expect(result.current.plans).toHaveLength(initialCount + 1);
    expect(result.current.getPlan(createdId)?.title).toBe('Guia de teste');

    await act(async () => {
      result.current.updatePlan(createdId, 'Guia atualizado', [{ title: 'T2', content: 'C2' }]);
    });
    expect(result.current.getPlan(createdId)?.title).toBe('Guia atualizado');

    await act(async () => {
      result.current.toggleActive(createdId);
    });
    expect(result.current.getPlan(createdId)?.active).toBe(false);

    await act(async () => {
      result.current.deletePlan(createdId);
    });
    expect(result.current.getPlan(createdId)).toBeUndefined();
    expect(result.current.plans).toHaveLength(initialCount);
  });

  it('finds a plan by its public id', async () => {
    const { result } = await renderHook(() => usePlans(), { wrapper });
    const [first] = result.current.plans;
    expect(result.current.getPlanByPublicId(first.publicId)?.id).toBe(first.id);
  });
});
