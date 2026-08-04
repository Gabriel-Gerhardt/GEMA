import { render, screen } from '@testing-library/react-native';
import { Text } from 'react-native';
import { Card } from './Card';

describe('Card', () => {
  it('renders its children', async () => {
    await render(
      <Card>
        <Text>Guia do Lucas</Text>
      </Card>,
    );
    expect(screen.getByText('Guia do Lucas')).toBeOnTheScreen();
  });
});
