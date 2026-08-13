import { render, screen, userEvent } from '@testing-library/react-native';
import { useNavigation } from '@react-navigation/native';
import { GalleryScreen } from './GalleryScreen';
import { usePlans } from '../state/PlansContext';

jest.mock('@react-navigation/native', () => ({
  ...jest.requireActual('@react-navigation/native'),
  useNavigation: jest.fn(),
}));
jest.mock('../state/PlansContext');

const PLANS = [
  { id: 'p1', publicId: 'abc123', title: 'Guia do Lucas', active: true, createdAt: '2026-06-12', sections: [] },
  { id: 'p3', publicId: 'ghi789', title: 'Rede Wi-Fi convidados', active: false, createdAt: '2026-06-01', sections: [] },
];

describe('GalleryScreen', () => {
  const navigate = jest.fn();
  beforeEach(() => {
    navigate.mockClear();
    (useNavigation as jest.Mock).mockReturnValue({ navigate });
    (usePlans as jest.Mock).mockReturnValue({ plans: PLANS });
  });

  it('lists all plans', async () => {
    await render(<GalleryScreen />);
    expect(screen.getByText('Guia do Lucas')).toBeOnTheScreen();
    expect(screen.getByText('Rede Wi-Fi convidados')).toBeOnTheScreen();
  });

  it('navigates to Plan Detail when a card is tapped', async () => {
    const user = userEvent.setup();
    await render(<GalleryScreen />);
    await user.press(screen.getByRole('button', { name: /Guia do Lucas/ }));
    expect(navigate).toHaveBeenCalledWith('PlanDetail', { planId: 'p1' });
  });

  it('shows an empty state instead of a blank list when there are no plans', async () => {
    (usePlans as jest.Mock).mockReturnValue({ plans: [] });
    await render(<GalleryScreen />);
    expect(screen.getByText('Nenhum plano ainda')).toBeOnTheScreen();
    // The create action must still be reachable from the empty state.
    expect(screen.getByRole('button', { name: '+ Criar plano' })).toBeOnTheScreen();
  });

  it('navigates to Create Plan from the bottom button', async () => {
    const user = userEvent.setup();
    await render(<GalleryScreen />);
    await user.press(screen.getByRole('button', { name: '+ Criar plano' }));
    expect(navigate).toHaveBeenCalledWith('CreatePlan');
  });
});
