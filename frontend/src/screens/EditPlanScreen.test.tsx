import { render, screen, userEvent } from '@testing-library/react-native';
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
    (usePlans as jest.Mock).mockReturnValue({
      getPlan: (id: string) => (id === 'p1' ? PLAN : undefined),
      updatePlan,
      deletePlan,
      toggleActive,
    });
  });

  it('pre-fills the title and existing sections', async () => {
    await render(<EditPlanScreen />);
    expect(screen.getByDisplayValue('Guia do Lucas')).toBeOnTheScreen();
    expect(screen.getByDisplayValue('Sobre mim')).toBeOnTheScreen();
    expect(screen.getByDisplayValue('O que ajuda')).toBeOnTheScreen();
    expect(screen.getByText(/abc123/)).toBeOnTheScreen();
  });

  it('toggles active state immediately', async () => {
    const user = userEvent.setup();
    await render(<EditPlanScreen />);
    await user.press(screen.getByRole('switch'));
    expect(toggleActive).toHaveBeenCalledWith('p1');
  });

  it('saves changes and goes back', async () => {
    const user = userEvent.setup();
    await render(<EditPlanScreen />);
    await user.press(screen.getByRole('button', { name: 'Salvar alterações' }));
    expect(updatePlan).toHaveBeenCalledWith('p1', 'Guia do Lucas', expect.any(Array));
    expect(goBack).toHaveBeenCalled();
  });

  it('deletes the plan and navigates to Gallery when the confirm dialog is accepted', async () => {
    const alertSpy = jest.spyOn(Alert, 'alert').mockImplementation((_title, _msg, buttons) => {
      const destructive = buttons?.find((b) => b.style === 'destructive');
      destructive?.onPress?.();
    });
    const user = userEvent.setup();
    await render(<EditPlanScreen />);
    await user.press(screen.getByRole('link', { name: 'Excluir plano' }));
    expect(deletePlan).toHaveBeenCalledWith('p1');
    expect(navigate).toHaveBeenCalledWith('GalleryScreen');
    alertSpy.mockRestore();
  });
});
