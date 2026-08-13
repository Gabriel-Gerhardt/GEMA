import { render, screen, userEvent, waitFor } from '@testing-library/react-native';
import { useNavigation, useRoute } from '@react-navigation/native';
import { PlanDetailScreen } from './PlanDetailScreen';
import { usePlans } from '../state/PlansContext';

jest.mock('@react-navigation/native', () => ({
  ...jest.requireActual('@react-navigation/native'),
  useNavigation: jest.fn(),
  useFocusEffect: jest.fn(),
  useRoute: jest.fn(),
}));
jest.mock('../state/PlansContext');

const PLAN = {
  id: 'p1',
  publicId: 'abc123',
  title: 'Guia do Lucas',
  active: true,
  createdAt: '2026-06-12',
  sections: [{ id: 's1', title: 'Sobre mim', content: 'Sou autista.' }],
};

describe('PlanDetailScreen', () => {
  const navigate = jest.fn();
  const goBack = jest.fn();
  beforeEach(() => {
    navigate.mockClear();
    goBack.mockClear();
    (useNavigation as jest.Mock).mockReturnValue({ navigate, goBack });
    (useRoute as jest.Mock).mockReturnValue({ params: { planId: 'p1' } });
    (usePlans as jest.Mock).mockReturnValue({
      fetchPlan: async (id: string) => {
        if (id !== 'p1') throw new Error('not found');
        return PLAN;
      },
    });
  });

  it('renders the plan title, status, and its sections read-only', async () => {
    await render(<PlanDetailScreen />);
    await waitFor(() => expect(screen.queryByText(/Carregando/)).not.toBeOnTheScreen());
    expect(screen.getByText('Guia do Lucas')).toBeOnTheScreen();
    expect(screen.getByText('Ativo')).toBeOnTheScreen();
    expect(screen.getByText('Sobre mim')).toBeOnTheScreen();
    expect(screen.getByText('Sou autista.')).toBeOnTheScreen();
  });

  it('navigates to Edit Plan', async () => {
    const user = userEvent.setup();
    await render(<PlanDetailScreen />);
    await waitFor(() => expect(screen.queryByText(/Carregando/)).not.toBeOnTheScreen());
    await user.press(screen.getByRole('button', { name: 'Editar plano' }));
    expect(navigate).toHaveBeenCalledWith('EditPlan', { planId: 'p1' });
  });

  it('goes back to Gallery from the back link', async () => {
    const user = userEvent.setup();
    await render(<PlanDetailScreen />);
    await waitFor(() => expect(screen.queryByText(/Carregando/)).not.toBeOnTheScreen());
    await user.press(screen.getByRole('link', { name: '← Galeria' }));
    expect(goBack).toHaveBeenCalled();
  });

  it('navigates to the Emergency Guide preview when the public link is tapped', async () => {
    const user = userEvent.setup();
    await render(<PlanDetailScreen />);
    await waitFor(() => expect(screen.queryByText(/Carregando/)).not.toBeOnTheScreen());
    await user.press(screen.getByRole('link', { name: 'Ver guia público abc123' }));
    expect(navigate).toHaveBeenCalledWith('EmergencyGuide', { publicId: 'abc123' });
  });
});
