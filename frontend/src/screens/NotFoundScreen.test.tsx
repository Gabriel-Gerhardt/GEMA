import { render, screen, userEvent } from '@testing-library/react-native';
import { useNavigation } from '@react-navigation/native';
import { NotFoundScreen } from './NotFoundScreen';

jest.mock('@react-navigation/native', () => ({
  ...jest.requireActual('@react-navigation/native'),
  useNavigation: jest.fn(),
}));

describe('NotFoundScreen', () => {
  const navigate = jest.fn();
  beforeEach(() => {
    navigate.mockClear();
    (useNavigation as jest.Mock).mockReturnValue({ navigate });
  });

  it('renders the not-found message', async () => {
    await render(<NotFoundScreen />);
    expect(screen.getByText('Página não encontrada')).toBeOnTheScreen();
  });

  it('navigates back to Landing', async () => {
    const user = userEvent.setup();
    await render(<NotFoundScreen />);
    await user.press(screen.getByRole('link', { name: 'Voltar ao início' }));
    expect(navigate).toHaveBeenCalledWith('Landing');
  });
});
