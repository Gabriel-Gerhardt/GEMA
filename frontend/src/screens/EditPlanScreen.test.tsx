import { render, screen, userEvent, waitFor } from '@testing-library/react-native';
import { Alert } from 'react-native';
import { useNavigation, useRoute } from '@react-navigation/native';
import { EditPlanScreen } from './EditPlanScreen';
import { usePlans } from '../state/PlansContext';

jest.mock('@react-navigation/native', () => ({
  ...jest.requireActual('@react-navigation/native'),
  useNavigation: jest.fn(),
  useRoute: jest.fn(),
}));
jest.mock('../state/PlansContext');

const PLAN = {
  id: 'p1',
  publicId: 'abc123',
  title: 'Guia do Lucas',
  active: true,
  createdAt: '2026-06-12',
  sections: [
    { id: 's1', title: 'Sobre mim', content: 'Sou autista.' },
    { id: 's2', title: 'O que ajuda', content: 'Fale devagar.' },
  ],
};

describe('EditPlanScreen', () => {
  const navigate = jest.fn();
  const goBack = jest.fn();
  const updatePlan = jest.fn();
  const deletePlan = jest.fn();
  const toggleActive = jest.fn();

  beforeEach(() => {
    [navigate, goBack, updatePlan, deletePlan, toggleActive].forEach((fn) => fn.mockClear());
    (useNavigation as jest.Mock).mockReturnValue({ navigate, goBack });
    (useRoute as jest.Mock).mockReturnValue({ params: { planId: 'p1' } });
    updatePlan.mockResolvedValue(undefined);
    deletePlan.mockResolvedValue(undefined);
    (usePlans as jest.Mock).mockReturnValue({
      fetchPlan: async (id: string) => {
        if (id !== 'p1') throw new Error('not found');
        return PLAN;
      },
      updatePlan,
      deletePlan,
      toggleActive,
    });
  });

  it('pre-fills the title and existing sections', async () => {
    await render(<EditPlanScreen />);
    await waitFor(() => expect(screen.queryByText(/Carregando/)).not.toBeOnTheScreen());
    expect(screen.getByDisplayValue('Guia do Lucas')).toBeOnTheScreen();
    expect(screen.getByDisplayValue('Sobre mim')).toBeOnTheScreen();
    expect(screen.getByDisplayValue('O que ajuda')).toBeOnTheScreen();
    expect(screen.getByText(/abc123/)).toBeOnTheScreen();
  });

  it('the active toggle edits the draft and is persisted on save', async () => {
    // It used to write straight through on tap. Folding it into the draft means
    // one save, and no surprise change to what the public sees from a stray tap.
    const user = userEvent.setup();
    await render(<EditPlanScreen />);
    await waitFor(() => expect(screen.queryByText(/Carregando/)).not.toBeOnTheScreen());

    expect(screen.getByText('Ativo')).toBeOnTheScreen();
    await user.press(screen.getByRole('switch'));
    expect(screen.getByText('Inativo')).toBeOnTheScreen();

    await user.press(screen.getByRole('button', { name: 'Salvar alterações' }));
    await waitFor(() =>
      expect(updatePlan).toHaveBeenCalledWith('p1', expect.objectContaining({ active: false })),
    );
  });

  it('saves changes and goes back', async () => {
    const user = userEvent.setup();
    await render(<EditPlanScreen />);
    await waitFor(() => expect(screen.queryByText(/Carregando/)).not.toBeOnTheScreen());
    await user.press(screen.getByRole('button', { name: 'Salvar alterações' }));
    await waitFor(() =>
      expect(updatePlan).toHaveBeenCalledWith(
        'p1',
        expect.objectContaining({ title: 'Guia do Lucas', active: true, sections: expect.any(Array) }),
      ),
    );
    await waitFor(() => expect(goBack).toHaveBeenCalled());
  });

  it('deletes the plan and navigates to Gallery when the confirm dialog is accepted', async () => {
    const alertSpy = jest.spyOn(Alert, 'alert').mockImplementation((_title, _msg, buttons) => {
      const destructive = buttons?.find((b) => b.style === 'destructive');
      destructive?.onPress?.();
    });
    const user = userEvent.setup();
    await render(<EditPlanScreen />);
    await waitFor(() => expect(screen.queryByText(/Carregando/)).not.toBeOnTheScreen());
    await user.press(screen.getByRole('link', { name: 'Excluir plano' }));
    await waitFor(() => expect(deletePlan).toHaveBeenCalledWith('p1'));
    await waitFor(() => expect(navigate).toHaveBeenCalledWith('GalleryScreen'));
    alertSpy.mockRestore();
  });
});
