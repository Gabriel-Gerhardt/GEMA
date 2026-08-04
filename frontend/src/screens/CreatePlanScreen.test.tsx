import { render, screen, userEvent } from '@testing-library/react-native';
import { Alert } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { CreatePlanScreen } from './CreatePlanScreen';
import { usePlans } from '../state/PlansContext';

jest.mock('@react-navigation/native', () => ({
  ...jest.requireActual('@react-navigation/native'),
  useNavigation: jest.fn(),
}));
jest.mock('../state/PlansContext');

describe('CreatePlanScreen', () => {
  const navigate = jest.fn();
  const createPlan = jest.fn();

  beforeEach(() => {
    navigate.mockClear();
    createPlan.mockClear();
    createPlan.mockReturnValue({ id: 'new-plan-id', publicId: 'xyz999' });
    (useNavigation as jest.Mock).mockReturnValue({ navigate });
    (usePlans as jest.Mock).mockReturnValue({ createPlan });
  });

  it('starts with two empty section blocks, matching the design mock', async () => {
    await render(<CreatePlanScreen />);
    expect(screen.getByText('Seção 1')).toBeOnTheScreen();
    expect(screen.getByText('Seção 2')).toBeOnTheScreen();
    expect(screen.getByText('2 seções')).toBeOnTheScreen();
  });

  it('adds a new section block', async () => {
    const user = userEvent.setup();
    await render(<CreatePlanScreen />);
    await user.press(screen.getByRole('button', { name: '+ Adicionar seção' }));
    expect(screen.getByText('Seção 3')).toBeOnTheScreen();
    expect(screen.getByText('3 seções')).toBeOnTheScreen();
  });

  it('removes a section block once the confirm dialog is accepted', async () => {
    const alertSpy = jest.spyOn(Alert, 'alert').mockImplementation((_title, _msg, buttons) => {
      const destructive = buttons?.find((b) => b.style === 'destructive');
      destructive?.onPress?.();
    });
    const user = userEvent.setup();
    await render(<CreatePlanScreen />);
    await user.press(screen.getAllByRole('button', { name: 'Remover seção' })[0]);
    expect(screen.getByText('1 seção')).toBeOnTheScreen();
    alertSpy.mockRestore();
  });

  it('creates the plan and navigates to Plan Created on submit', async () => {
    const user = userEvent.setup();
    await render(<CreatePlanScreen />);
    await user.type(screen.getByPlaceholderText('Guia do Lucas'), 'Guia da Ana');
    await user.press(screen.getByRole('button', { name: 'Criar plano' }));
    expect(createPlan).toHaveBeenCalledWith('Guia da Ana', expect.any(Array));
    expect(navigate).toHaveBeenCalledWith('PlanCreated', { planId: 'new-plan-id' });
  });
});
