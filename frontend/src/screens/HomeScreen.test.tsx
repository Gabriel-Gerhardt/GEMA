import { render, screen, userEvent } from '@testing-library/react-native';
import { useNavigation } from '@react-navigation/native';
import { HomeScreen } from './HomeScreen';
import { usePlans } from '../state/PlansContext';

jest.mock('@react-navigation/native', () => ({
  ...jest.requireActual('@react-navigation/native'),
  useNavigation: jest.fn(),
}));
jest.mock('../state/PlansContext');

const PLANS = [
  { id: 'p1', publicId: 'abc123', title: 'Guia do Lucas', active: true, createdAt: '2026-06-12', sections: [] },
  { id: 'p2', publicId: 'def456', title: 'Contato de emergência', active: true, createdAt: '2026-06-05', sections: [] },
];

describe('HomeScreen', () => {
  const navigate = jest.fn();
  beforeEach(() => {
    navigate.mockClear();
    (useNavigation as jest.Mock).mockReturnValue({ navigate });
    (usePlans as jest.Mock).mockReturnValue({ plans: PLANS });
  });

  it('greets the user and lists recent activity', async () => {
    await render(<HomeScreen />);
    expect(screen.getByText('Olá, Eduarda.')).toBeOnTheScreen();
    expect(screen.getByText('Guia do Lucas')).toBeOnTheScreen();
    expect(screen.getByText('Contato de emergência')).toBeOnTheScreen();
  });

  it('navigates to CreatePlan from the "Criar plano" tile', async () => {
    const user = userEvent.setup();
    await render(<HomeScreen />);
    await user.press(screen.getByRole('button', { name: 'Criar plano' }));
    expect(navigate).toHaveBeenCalledWith('CreatePlan');
  });
});
