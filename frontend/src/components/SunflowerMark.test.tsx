import { render, screen } from '@testing-library/react-native';
import { SunflowerMark, SunflowerWordmark } from './SunflowerMark';

describe('SunflowerMark', () => {
  it('renders the sunflower svg at the requested size', async () => {
    await render(<SunflowerMark size={48} />);
    const svg = screen.getByTestId('sunflower-mark');
    expect(svg.props.width).toBe(48);
    expect(svg.props.height).toBe(48);
  });

  it('defaults to size 32 when no size is given', async () => {
    await render(<SunflowerMark />);
    const svg = screen.getByTestId('sunflower-mark');
    expect(svg.props.width).toBe(32);
  });
});

describe('SunflowerWordmark', () => {
  it('renders the GEMA wordmark text next to the mark', async () => {
    await render(<SunflowerWordmark />);
    expect(screen.getByText('GEMA')).toBeOnTheScreen();
  });
});
