import { Text } from 'react-native';
import { act, render, screen, waitFor } from '@testing-library/react-native';
import { PlansProvider, usePlans } from './PlansContext';
import * as api from '../api/endpoints';
import { useAuth } from './AuthContext';

jest.mock('../api/endpoints');
jest.mock('./AuthContext');

const mockApi = api as jest.Mocked<typeof api>;
const mockUseAuth = useAuth as jest.Mock;

const ROW = {
  publicId: 'abc123',
  title: 'Guia do Lucas',
  isActive: true,
  createdAt: '2026-06-12T09:30:00',
  updatedAt: '2026-06-12T09:30:00',
};

const PLAN = {
  publicId: 'abc123',
  title: 'Guia do Lucas',
  content: null,
  ownerName: 'Lucas',
  emergencyContactName: 'Ana',
  emergencyContactPhone: '51999990000',
  isActive: true,
  createdAt: '2026-06-12T09:30:00',
  updatedAt: '2026-06-12T09:30:00',
};

const SECTION = {
  id: 7,
  qrcodePublicId: 'abc123',
  title: 'Sobre mim',
  content: 'Sou autista.',
  sortOrder: 0,
  createdAt: '2026-06-12T09:30:00',
  updatedAt: '2026-06-12T09:30:00',
};

function page<T>(content: T[]) {
  return { content, totalElements: content.length, number: 0, size: 50, last: true };
}

let plans: ReturnType<typeof usePlans>;

function Probe() {
  plans = usePlans();
  return <Text>{plans.isLoading ? 'loading' : `count:${plans.plans.length}`}</Text>;
}

async function renderProvider() {
  await render(
    <PlansProvider>
      <Probe />
    </PlansProvider>,
  );
  await waitFor(() => expect(screen.queryByText('loading')).not.toBeOnTheScreen());
}

describe('PlansContext', () => {
  const onUnauthorized = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockUseAuth.mockReturnValue({ isSignedIn: true, onUnauthorized });
    mockApi.listPlans.mockResolvedValue(page([ROW]));
  });

  it('loads the caller’s plans when a session starts', async () => {
    await renderProvider();

    expect(mockApi.listPlans).toHaveBeenCalled();
    expect(plans.plans).toHaveLength(1);
    expect(plans.plans[0].title).toBe('Guia do Lucas');
    // The screens render a plain date, not the API's date-time.
    expect(plans.plans[0].createdAt).toBe('2026-06-12');
  });

  it('exposes a load failure instead of showing an empty gallery', async () => {
    mockApi.listPlans.mockRejectedValue(new Error('Não foi possível falar com o servidor.'));

    await renderProvider();

    expect(plans.error).toBe('Não foi possível falar com o servidor.');
    expect(plans.plans).toHaveLength(0);
  });

  it('drops the list when the session ends', async () => {
    // Leaving one user's plans on screen after sign-out would be a disclosure,
    // not just a stale render.
    mockUseAuth.mockReturnValue({ isSignedIn: false, onUnauthorized });

    await renderProvider();

    expect(mockApi.listPlans).not.toHaveBeenCalled();
    expect(plans.plans).toHaveLength(0);
  });

  it('fetches a plan together with its sections', async () => {
    mockApi.getPlan.mockResolvedValue(PLAN);
    mockApi.getPlanSections.mockResolvedValue([SECTION]);
    await renderProvider();

    const plan = await act(async () => plans.fetchPlan('abc123'));

    expect(plan.title).toBe('Guia do Lucas');
    expect(plan.ownerName).toBe('Lucas');
    expect(plan.emergencyContactPhone).toBe('51999990000');
    expect(plan.sections).toEqual([{ id: '7', title: 'Sobre mim', content: 'Sou autista.' }]);
  });

  it('creates a plan and its sections in a single call', async () => {
    // Two calls could leave an empty plan behind if the second failed.
    mockApi.createPlan.mockResolvedValue(PLAN);
    await renderProvider();

    await act(async () => {
      await plans.createPlan({ title: 'Guia do Lucas', sections: [{ title: 'Sobre mim', content: 'Sou autista.' }] });
    });

    expect(mockApi.createPlan).toHaveBeenCalledWith(
      expect.objectContaining({
        title: 'Guia do Lucas',
        sections: [{ title: 'Sobre mim', content: 'Sou autista.' }],
      }),
      expect.anything(),
    );
  });

  it('reloads the list after creating', async () => {
    mockApi.createPlan.mockResolvedValue(PLAN);
    await renderProvider();
    mockApi.listPlans.mockClear();

    await act(async () => {
      await plans.createPlan({ title: 'Novo', sections: [] });
    });

    expect(mockApi.listPlans).toHaveBeenCalled();
  });

  it('saves an edit as a plan update plus a section replacement', async () => {
    mockApi.updatePlan.mockResolvedValue(PLAN);
    mockApi.replaceSections.mockResolvedValue([SECTION]);
    await renderProvider();

    await act(async () => {
      await plans.updatePlan('abc123', {
        title: 'Renomeado',
        active: false,
        sections: [{ title: 'Sobre mim', content: 'Sou autista.' }],
      });
    });

    expect(mockApi.updatePlan).toHaveBeenCalledWith(
      'abc123',
      { title: 'Renomeado', isActive: false },
      expect.anything(),
    );
    expect(mockApi.replaceSections).toHaveBeenCalledWith(
      'abc123',
      [{ title: 'Sobre mim', content: 'Sou autista.' }],
      expect.anything(),
    );
  });

  it('deletes a plan and refreshes', async () => {
    mockApi.deletePlan.mockResolvedValue(undefined);
    await renderProvider();
    mockApi.listPlans.mockClear();

    await act(async () => {
      await plans.deletePlan('abc123');
    });

    expect(mockApi.deletePlan).toHaveBeenCalledWith('abc123', expect.anything());
    expect(mockApi.listPlans).toHaveBeenCalled();
  });

  it('toggling active keeps the plan’s title', async () => {
    // The update endpoint replaces the whole plan, so a toggle that forgot the
    // title would blank it.
    mockApi.getPlan.mockResolvedValue(PLAN);
    mockApi.updatePlan.mockResolvedValue({ ...PLAN, isActive: false });
    await renderProvider();

    await act(async () => {
      await plans.toggleActive('abc123', false);
    });

    expect(mockApi.updatePlan).toHaveBeenCalledWith(
      'abc123',
      { title: 'Guia do Lucas', isActive: false },
      expect.anything(),
    );
  });
});
