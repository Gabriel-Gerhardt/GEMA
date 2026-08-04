import { render, screen } from '@testing-library/react-native';
import { SectionReadItem } from './SectionReadItem';

describe('SectionReadItem', () => {
  it('renders the section title and content', async () => {
    await render(<SectionReadItem title="Sobre mim" content="Sou autista." />);
    expect(screen.getByText('Sobre mim')).toBeOnTheScreen();
    expect(screen.getByText('Sou autista.')).toBeOnTheScreen();
  });
});
