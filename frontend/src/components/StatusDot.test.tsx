import { render, screen } from '@testing-library/react-native';
import { StatusDot } from './StatusDot';

describe('StatusDot', () => {
  it('is labeled active when active', async () => {
    await render(<StatusDot active />);
    expect(screen.getByLabelText('ativo')).toBeOnTheScreen();
  });

  it('is labeled inactive when not active', async () => {
    await render(<StatusDot active={false} />);
    expect(screen.getByLabelText('inativo')).toBeOnTheScreen();
  });
});
