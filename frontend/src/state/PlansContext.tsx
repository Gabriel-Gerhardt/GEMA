import { createContext, useCallback, useContext, useMemo, useReducer, type ReactNode } from 'react';
import { SEED_PLANS } from '../mocks/plans';
import { buildPlan, plansReducer } from './plansReducer';
import type { Plan, Section } from './types';

interface PlansContextValue {
  plans: Plan[];
  getPlan: (id: string) => Plan | undefined;
  getPlanByPublicId: (publicId: string) => Plan | undefined;
  createPlan: (title: string, sections: Pick<Section, 'title' | 'content'>[]) => Plan;
  updatePlan: (id: string, title: string, sections: (Pick<Section, 'title' | 'content'> & Partial<Pick<Section, 'id'>>)[]) => void;
  deletePlan: (id: string) => void;
  toggleActive: (id: string) => void;
}

const PlansContext = createContext<PlansContextValue | null>(null);

export function PlansProvider({ children }: { children: ReactNode }) {
  const [plans, dispatch] = useReducer(plansReducer, SEED_PLANS);

  const getPlan = useCallback((id: string) => plans.find((p) => p.id === id), [plans]);
  const getPlanByPublicId = useCallback(
    (publicId: string) => plans.find((p) => p.publicId === publicId),
    [plans],
  );

  const createPlan = useCallback((title: string, sections: Pick<Section, 'title' | 'content'>[]) => {
    const plan = buildPlan(title, sections);
    dispatch({ type: 'ADD_PLAN', plan });
    return plan;
  }, []);

  const updatePlan = useCallback(
    (id: string, title: string, sections: (Pick<Section, 'title' | 'content'> & Partial<Pick<Section, 'id'>>)[]) => {
      dispatch({ type: 'UPDATE_PLAN', id, title, sections });
    },
    [],
  );

  const deletePlan = useCallback((id: string) => dispatch({ type: 'DELETE_PLAN', id }), []);
  const toggleActive = useCallback((id: string) => dispatch({ type: 'TOGGLE_ACTIVE', id }), []);

  const value = useMemo(
    () => ({ plans, getPlan, getPlanByPublicId, createPlan, updatePlan, deletePlan, toggleActive }),
    [plans, getPlan, getPlanByPublicId, createPlan, updatePlan, deletePlan, toggleActive],
  );

  return <PlansContext.Provider value={value}>{children}</PlansContext.Provider>;
}

export function usePlans(): PlansContextValue {
  const ctx = useContext(PlansContext);
  if (!ctx) throw new Error('usePlans must be used within a PlansProvider');
  return ctx;
}
