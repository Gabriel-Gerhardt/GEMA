import { render, screen, userEvent } from '@testing-library/react-native';
import { Linking } from 'react-native';
import { useNavigation, useRoute } from '@react-navigation/native';
import { EmergencyGuideScreen } from './EmergencyGuideScreen';
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
  ownerName: 'Lucas',
  sections: [
    { id: 's1', title: 'Sobre mim', content: 'Sou autista e posso ficar sobrecarregado.' },
    { id: 's2', title: 'O que ajuda', content: 'Fale devagar e com calma.' },
    { id: 's3', title: 'Em uma emergência', content: 'Ana — minha mãe · (51) 99999-0000' },
  ],
};

describe('EmergencyGuideScreen', () => {
  const goBack = jest.fn();

  beforeEach(() => {
    goBack.mockClear();
    (useNavigation as jest.Mock).mockReturnValue({ goBack, canGoBack: () => true });
    (useRoute as jest.Mock).mockReturnValue({ params: { publicId: 'abc123' } });
    (usePlans as jest.Mock).mockReturnValue({
      getPlanByPublicId: (id: string) => (id === 'abc123' ? PLAN : undefined),
    });
    // mockClear as well as re-stubbing: spyOn returns the SAME spy when the
    // method is already spied, so call history would otherwise leak between tests.
    jest.spyOn(Linking, 'openURL').mockClear().mockResolvedValue(undefined as never);
  });

  afterEach(() => jest.restoreAllMocks());

  it('renders the greeting from the first section, then the remaining sections', async () => {
    await render(<EmergencyGuideScreen />);
    expect(screen.getByText('Guia de apoio')).toBeOnTheScreen();
    expect(screen.getByText('Olá, meu nome é Lucas.')).toBeOnTheScreen();
    expect(screen.getByText('Sou autista e posso ficar sobrecarregado.')).toBeOnTheScreen();
    expect(screen.queryByText('Sobre mim')).not.toBeOnTheScreen();
    expect(screen.getByText('O que ajuda')).toBeOnTheScreen();
    expect(screen.getByText('Em uma emergência')).toBeOnTheScreen();
  });

  it('falls back to rendering every section generically when the plan has no ownerName', async () => {
    (usePlans as jest.Mock).mockReturnValue({
      getPlanByPublicId: () => ({ ...PLAN, ownerName: undefined }),
    });
    await render(<EmergencyGuideScreen />);
    expect(screen.queryByText(/Olá, meu nome é/)).not.toBeOnTheScreen();
    expect(screen.getByText('Sobre mim')).toBeOnTheScreen();
    expect(screen.getByText('O que ajuda')).toBeOnTheScreen();
  });

  it('opens a tel: link with the phone number found in the emergency section', async () => {
    const user = userEvent.setup();
    await render(<EmergencyGuideScreen />);
    await user.press(screen.getByRole('button', { name: 'Ligar agora' }));
    expect(Linking.openURL).toHaveBeenCalledWith('tel:51999990000');
  });

  it('shows a not-found state instead of a blank screen when the plan is missing', async () => {
    // This is the surface a stranger reaches by scanning a code; rendering
    // nothing at all was the worst possible outcome there.
    (usePlans as jest.Mock).mockReturnValue({ getPlanByPublicId: () => undefined });
    await render(<EmergencyGuideScreen />);
    expect(screen.queryByText('Guia de apoio')).not.toBeOnTheScreen();
    expect(screen.getByText('Guia não encontrado')).toBeOnTheScreen();
  });

  it('treats a deactivated plan as unavailable', async () => {
    // The API returns 404 for a deactivated plan; the toggle only means
    // something if the guide actually stops being shown.
    (usePlans as jest.Mock).mockReturnValue({ getPlanByPublicId: () => ({ ...PLAN, active: false }) });
    await render(<EmergencyGuideScreen />);
    expect(screen.getByText('Guia não encontrado')).toBeOnTheScreen();
    expect(screen.queryByText('Ligar agora')).not.toBeOnTheScreen();
  });

  it('finds the emergency section even when it is not the last one', async () => {
    // Detection used to require the emergency section to be last, so an owner
    // who ordered their sections differently silently lost the call button.
    (usePlans as jest.Mock).mockReturnValue({
      getPlanByPublicId: () => ({
        ...PLAN,
        ownerName: undefined,
        sections: [
          { id: 's3', title: 'Em uma emergência', content: 'Ana — minha mãe · (51) 99999-0000' },
          { id: 's1', title: 'Sobre mim', content: 'Sou autista e posso ficar sobrecarregado.' },
        ],
      }),
    });
    const user = userEvent.setup();
    await render(<EmergencyGuideScreen />);
    await user.press(screen.getByRole('button', { name: 'Ligar agora' }));
    expect(Linking.openURL).toHaveBeenCalledWith('tel:51999990000');
  });

  it('disables the call button when the emergency section has no phone number', async () => {
    (usePlans as jest.Mock).mockReturnValue({
      getPlanByPublicId: () => ({
        ...PLAN,
        sections: [{ id: 's3', title: 'Em uma emergência', content: 'Procure um adulto responsável.' }],
      }),
    });
    const user = userEvent.setup();
    await render(<EmergencyGuideScreen />);
    await user.press(screen.getByRole('button', { name: 'Ligar agora' }));
    expect(Linking.openURL).not.toHaveBeenCalled();
  });
});
