import { render, screen } from '@testing-library/react-native';
import { QrPlaceholder } from './QrPlaceholder';

describe('QrPlaceholder', () => {
  it('renders a labeled placeholder box at the requested size', async () => {
    await render(<QrPlaceholder size={150} />);
    const box = screen.getByLabelText('espaço reservado para o código QR');
    expect(box).toHaveStyle({ width: 150, height: 150 });
  });
});
