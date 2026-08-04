import { render, screen, userEvent } from '@testing-library/react-native';
import { useNavigation, useRoute } from '@react-navigation/native';
import * as Clipboard from 'expo-clipboard';
import { PlanCreatedScreen } from './PlanCreatedScreen';
import { usePlans } from '../state/PlansContext';

jest.mock('@react-navigation/native', () => ({
  ...jest.requireActual('@react-navigation/native'),
  useNavigation: jest.fn(),
  useRoute: jest.fn(),
}));
jest.mock('../state/PlansContext');
jest.mock('expo-clipboard', () => ({ setStringAsync: jest.fn() }));

const PLAN = {
  id: 'new-plan-id',
  publicId: 'xyz999',
  title: 'Guia da Ana',
  active: true,
  createdAt: '2026-08-01',
  sections: [],
};

describe('PlanCreatedScreen', () => {
  const navigate = jest.fn();

  beforeEach(() => {
    navigate.mockClear();
    (useNavigation as jest.Mock).mockReturnValue({ navigate });
    (useRoute as jest.Mock).mockReturnValue({ params: { planId: 'new-plan-id' } });
    (usePlans as jest.Mock).mockReturnValue({ getPlan: (id: string) => (id === 'new-plan-id' ? PLAN : undefined) });
    (Clipboard.setStringAsync as jest.Mock).mockClear();
  });

  it('shows the success message and the shareable link', async () => {
    await render(<PlanCreatedScreen />);
    expect(screen.getByText('Plano criado')).toBeOnTheScreen();
    expect(screen.getByText('gema.app/q/xyz999')).toBeOnTheScreen();
  });

  it('copies the link to the clipboard', async () => {
    const user = userEvent.setup();
    await render(<PlanCreatedScreen />);
    await user.press(screen.getByRole('button', { name: 'Copiar link' }));
    expect(Clipboard.setStringAsync).toHaveBeenCalledWith('gema.app/q/xyz999');
  });

  it('navigates to the Emergency Guide preview when the link is tapped', async () => {
    const user = userEvent.setup();
    await render(<PlanCreatedScreen />);
    await user.press(screen.getByRole('link', { name: 'gema.app/q/xyz999' }));
    expect(navigate).toHaveBeenCalledWith('EmergencyGuide', { publicId: 'xyz999' });
  });
});
