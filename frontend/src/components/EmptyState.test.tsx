import { render, screen, userEvent } from '@testing-library/react-native';
import { EmptyState } from './EmptyState';

describe('EmptyState', () => {
  it('renders the title and message', async () => {
    await render(<EmptyState title="Nenhum plano ainda" message="Crie seu primeiro plano." />);
    expect(screen.getByText('Nenhum plano ainda')).toBeOnTheScreen();
    expect(screen.getByText('Crie seu primeiro plano.')).toBeOnTheScreen();
  });

  it('omits the action link when no action is given', async () => {
    await render(<EmptyState title="Nada aqui" message="Sem conteúdo." />);
    expect(screen.queryByRole('link')).not.toBeOnTheScreen();
  });

  it('calls onAction when the link is pressed', async () => {
    const onAction = jest.fn();
    const user = userEvent.setup();
    await render(
      <EmptyState title="Nada aqui" message="Sem conteúdo." actionLabel="Voltar" onAction={onAction} />,
    );
    await user.press(screen.getByRole('link', { name: 'Voltar' }));
    expect(onAction).toHaveBeenCalled();
  });
});
