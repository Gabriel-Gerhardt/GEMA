import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import * as api from '../api/endpoints';
import type { QrcodeResponse, SectionResponse, UserQrcodeResponse } from '../api/types';
import { useAuth } from './AuthContext';
import type { Plan, Section } from './types';

/** The API's date-times are ISO; the screens render a plain date. */
function toDisplayDate(isoDateTime: string): string {
  return isoDateTime.slice(0, 10);
}

function toSection(section: SectionResponse): Section {
  return { id: String(section.id), title: section.title, content: section.content };
}

/**
 * A row from the plan list. It has no sections: the list endpoint deliberately
 * does not carry them, so a gallery of twenty plans is one request rather than
 * twenty-one. Screens that render section content fetch the plan in full.
 */
function toSummary(row: UserQrcodeResponse): Plan {
  return {
    id: row.publicId,
    publicId: row.publicId,
    title: row.title,
    active: row.isActive,
    createdAt: toDisplayDate(row.createdAt),
    sections: [],
  };
}

function toPlan(plan: QrcodeResponse, sections: SectionResponse[]): Plan {
  return {
    id: plan.publicId,
    publicId: plan.publicId,
    title: plan.title,
    active: plan.isActive,
    createdAt: toDisplayDate(plan.createdAt),
    sections: sections.map(toSection),
    ownerName: plan.ownerName ?? undefined,
    emergencyContactName: plan.emergencyContactName ?? undefined,
    emergencyContactPhone: plan.emergencyContactPhone ?? undefined,
  };
}

interface CreatePlanInput {
  title: string;
  sections: Pick<Section, 'title' | 'content'>[];
  ownerName?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
}

interface PlansContextValue {
  plans: Plan[];
  isLoading: boolean;
  error: string | null;
  reload: () => Promise<void>;
  /** Fetches one plan with its sections; the list only carries summaries. */
  fetchPlan: (publicId: string) => Promise<Plan>;
  createPlan: (input: CreatePlanInput) => Promise<Plan>;
  updatePlan: (
    publicId: string,
    input: { title: string; active: boolean; sections: Pick<Section, 'title' | 'content'>[] },
  ) => Promise<void>;
  deletePlan: (publicId: string) => Promise<void>;
  toggleActive: (publicId: string, active: boolean) => Promise<void>;
}

const PlansContext = createContext<PlansContextValue | null>(null);

export function PlansProvider({ children }: { children: ReactNode }) {
  const { isSignedIn, onUnauthorized } = useAuth();
  const [plans, setPlans] = useState<Plan[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const opts = useMemo(() => ({ onUnauthorized }), [onUnauthorized]);

  const reload = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const page = await api.listPlans(0, 50, opts);
      setPlans(page.content.map(toSummary));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Não foi possível carregar seus planos.');
    } finally {
      setIsLoading(false);
    }
  }, [opts]);

  // The list belongs to a session, so it is loaded when one starts and dropped
  // when it ends — leaving one signed-in user's plans on screen after sign-out
  // would be a disclosure, not just a stale render.
  useEffect(() => {
    if (isSignedIn) {
      void reload();
    } else {
      setPlans([]);
      setError(null);
    }
  }, [isSignedIn, reload]);

  const fetchPlan = useCallback(
    async (publicId: string) => {
      const [plan, sections] = await Promise.all([
        api.getPlan(publicId, opts),
        api.getPlanSections(publicId, opts),
      ]);
      return toPlan(plan, sections);
    },
    [opts],
  );

  const createPlan = useCallback(
    async (input: CreatePlanInput) => {
      // One call, one transaction: creating the plan and then its sections
      // separately could leave an empty plan behind if the second failed.
      const created = await api.createPlan(
        {
          title: input.title,
          ownerName: input.ownerName,
          emergencyContactName: input.emergencyContactName,
          emergencyContactPhone: input.emergencyContactPhone,
          sections: input.sections.map((s) => ({ title: s.title, content: s.content })),
        },
        opts,
      );
      await reload();
      return toPlan(created, []);
    },
    [opts, reload],
  );

  const updatePlan = useCallback(
    async (
      publicId: string,
      input: { title: string; active: boolean; sections: Pick<Section, 'title' | 'content'>[] },
    ) => {
      await api.updatePlan(publicId, { title: input.title, isActive: input.active }, opts);
      await api.replaceSections(
        publicId,
        input.sections.map((s) => ({ title: s.title, content: s.content })),
        opts,
      );
      await reload();
    },
    [opts, reload],
  );

  const deletePlan = useCallback(
    async (publicId: string) => {
      await api.deletePlan(publicId, opts);
      await reload();
    },
    [opts, reload],
  );

  const toggleActive = useCallback(
    async (publicId: string, active: boolean) => {
      const current = await api.getPlan(publicId, opts);
      await api.updatePlan(publicId, { title: current.title, isActive: active }, opts);
      await reload();
    },
    [opts, reload],
  );

  const value = useMemo(
    () => ({ plans, isLoading, error, reload, fetchPlan, createPlan, updatePlan, deletePlan, toggleActive }),
    [plans, isLoading, error, reload, fetchPlan, createPlan, updatePlan, deletePlan, toggleActive],
  );

  return <PlansContext.Provider value={value}>{children}</PlansContext.Provider>;
}

export function usePlans(): PlansContextValue {
  const ctx = useContext(PlansContext);
  if (!ctx) throw new Error('usePlans must be used within a PlansProvider');
  return ctx;
}
