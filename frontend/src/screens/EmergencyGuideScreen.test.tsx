import { render, screen, userEvent, waitFor } from '@testing-library/react-native';
import { Linking } from 'react-native';
import { useNavigation, useRoute } from '@react-navigation/native';
import { EmergencyGuideScreen } from './EmergencyGuideScreen';
import * as api from '../api/endpoints';
import { ApiError, NetworkError } from '../api/client';

jest.mock('@react-navigation/native', () => ({
  ...jest.requireActual('@react-navigation/native'),
  useNavigation: jest.fn(),
  useRoute: jest.fn(),
}));
jest.mock('../api/endpoints');

const mockApi = api as jest.Mocked<typeof api>;

const PLAN = {
  publicId: 'abc123',
  title: 'Guia do Lucas',
  content: null,
  ownerName: 'Lucas',
  emergencyContactName: null,
  emergencyContactPhone: null,
  isActive: true,
  createdAt: '2026-06-12T09:00:00',
  updatedAt: '2026-06-12T09:00:00',
};

function section(id: number, title: string, content: string, sortOrder: number) {
  return {
    id,
    qrcodePublicId: 'abc123',
    title,
    content,
    sortOrder,
    createdAt: '2026-06-12T09:00:00',
    updatedAt: '2026-06-12T09:00:00',
  };
}

const SECTIONS = [
  section(1, 'Sobre mim', 'Sou autista e posso ficar sobrecarregado.', 0),
  section(2, 'O que ajuda', 'Fale devagar e com calma.', 1),
  section(3, 'Em uma emergência', 'Ana — minha mãe · (51) 99999-0000', 2),
];

async function renderGuide() {
  await render(<EmergencyGuideScreen />);
  await waitFor(() => expect(screen.queryByText(/Carregando/)).not.toBeOnTheScreen());
}

describe('EmergencyGuideScreen', () => {
  const goBack = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    (useNavigation as jest.Mock).mockReturnValue({ goBack, canGoBack: () => true });
    (useRoute as jest.Mock).mockReturnValue({ params: { publicId: 'abc123' } });
    mockApi.getPublicPlan.mockResolvedValue(PLAN);
    mockApi.getPublicSections.mockResolvedValue(SECTIONS);
    jest.spyOn(Linking, 'openURL').mockClear().mockResolvedValue(undefined as never);
  });

  afterEach(() => jest.restoreAllMocks());

  it('reads from the public endpoints, which carry no token', async () => {
    // Whoever scanned this has no account; sending one would be meaningless and
    // requiring one would defeat the product.
    await renderGuide();

    expect(mockApi.getPublicPlan).toHaveBeenCalledWith('abc123');
    expect(mockApi.getPublicSections).toHaveBeenCalledWith('abc123');
  });

  it('renders the greeting from the first section, then the remaining sections', async () => {
    await renderGuide();

    expect(screen.getByText('Guia de apoio')).toBeOnTheScreen();
    expect(screen.getByText('Olá, meu nome é Lucas.')).toBeOnTheScreen();
    expect(screen.getByText('Sou autista e posso ficar sobrecarregado.')).toBeOnTheScreen();
    expect(screen.queryByText('Sobre mim')).not.toBeOnTheScreen();
    expect(screen.getByText('O que ajuda')).toBeOnTheScreen();
    expect(screen.getByText('Em uma emergência')).toBeOnTheScreen();
  });

  it('falls back to rendering every section generically when there is no ownerName', async () => {
    mockApi.getPublicPlan.mockResolvedValue({ ...PLAN, ownerName: null });

    await renderGuide();

    expect(screen.queryByText(/Olá, meu nome é/)).not.toBeOnTheScreen();
    expect(screen.getByText('Sobre mim')).toBeOnTheScreen();
  });

  it('opens a tel: link with the phone number found in the emergency section', async () => {
    const user = userEvent.setup();
    await renderGuide();

    await user.press(screen.getByRole('button', { name: 'Ligar agora' }));

    expect(Linking.openURL).toHaveBeenCalledWith('tel:51999990000');
  });

  it('prefers the structured contact phone over scanning the section text', async () => {
    // The call action is the one thing here that has to work under pressure; it
    // must not depend on how the prose was punctuated.
    mockApi.getPublicPlan.mockResolvedValue({
      ...PLAN,
      emergencyContactName: 'Ana — minha mãe',
      emergencyContactPhone: '(51) 98888-1111',
    });
    const user = userEvent.setup();
    await renderGuide();

    await user.press(screen.getByRole('button', { name: 'Ligar agora' }));

    expect(Linking.openURL).toHaveBeenCalledWith('tel:51988881111');
  });

  it('finds the emergency section even when it is not the last one', async () => {
    mockApi.getPublicPlan.mockResolvedValue({ ...PLAN, ownerName: null });
    mockApi.getPublicSections.mockResolvedValue([
      section(3, 'Em uma emergência', 'Ana — minha mãe · (51) 99999-0000', 0),
      section(1, 'Sobre mim', 'Sou autista.', 1),
    ]);
    const user = userEvent.setup();
    await renderGuide();

    await user.press(screen.getByRole('button', { name: 'Ligar agora' }));

    expect(Linking.openURL).toHaveBeenCalledWith('tel:51999990000');
  });

  it('disables the call button when the emergency section has no phone number', async () => {
    mockApi.getPublicSections.mockResolvedValue([
      section(3, 'Em uma emergência', 'Procure um adulto responsável.', 0),
    ]);
    const user = userEvent.setup();
    await renderGuide();

    await user.press(screen.getByRole('button', { name: 'Ligar agora' }));

    expect(Linking.openURL).not.toHaveBeenCalled();
  });

  it('shows a not-found state when the plan is gone or deactivated', async () => {
    // The API answers 404 for both. Rendering nothing at all — which this
    // screen used to do — is the worst outcome on the surface a stranger
    // reaches by scanning a code.
    mockApi.getPublicPlan.mockRejectedValue(new ApiError(404, 'Não encontramos o que você procurava.'));
    mockApi.getPublicSections.mockRejectedValue(new ApiError(404, 'Não encontramos o que você procurava.'));

    await renderGuide();

    expect(screen.getByText('Guia não encontrado')).toBeOnTheScreen();
    expect(screen.queryByText('Ligar agora')).not.toBeOnTheScreen();
  });

  it('offers a retry when the guide fails to load for a transport reason', async () => {
    // Bad signal is not the same as "this plan does not exist", and the person
    // reading this may be standing next to someone who needs help.
    mockApi.getPublicPlan.mockRejectedValue(new NetworkError());
    mockApi.getPublicSections.mockRejectedValue(new NetworkError());

    await renderGuide();

    expect(screen.getByText('Não foi possível carregar')).toBeOnTheScreen();
    expect(screen.getByRole('link', { name: 'Tentar de novo' })).toBeOnTheScreen();
  });
});
