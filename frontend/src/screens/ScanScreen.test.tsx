import { cleanup, render, screen, userEvent } from '@testing-library/react-native';
import { useNavigation } from '@react-navigation/native';
import { useCameraPermissions } from 'expo-camera';
import { ScanScreen } from './ScanScreen';
import { isWebPlatform } from '../lib/platform';

// The Jest preset runs this suite once per platform, so which platform the
// screen believes it is on has to be stated rather than assigned.
jest.mock('../lib/platform');

jest.mock('@react-navigation/native', () => ({
  ...jest.requireActual('@react-navigation/native'),
  useNavigation: jest.fn(),
}));

// The real CameraView needs a native view. The stand-in renders a pressable
// that fires the scan callback with whatever the test has queued, so scanning
// is driven through the normal render/interact flow rather than by holding on
// to props captured during an earlier render.
let mockQueuedScan = '';
let mockCameraSettings: unknown = null;
jest.mock('expo-camera', () => {
  const { Pressable, Text } = jest.requireActual('react-native');
  return {
    useCameraPermissions: jest.fn(),
    CameraView: ({ onBarcodeScanned, barcodeScannerSettings }: Record<string, any>) => {
      mockCameraSettings = barcodeScannerSettings;
      return (
        <Pressable role="button" aria-label="__camera__" onPress={() => onBarcodeScanned({ type: 'qr', data: mockQueuedScan })}>
          <Text>__camera__</Text>
        </Pressable>
      );
    },
  };
});

const mockPermissions = useCameraPermissions as jest.Mock;

function granted() {
  mockPermissions.mockReturnValue([{ granted: true, canAskAgain: true }, jest.fn()]);
}

describe('ScanScreen', () => {
  const navigate = jest.fn();
  const goBack = jest.fn();

  beforeEach(() => {
    navigate.mockClear();
    goBack.mockClear();
    mockQueuedScan = '';
    mockCameraSettings = null;
    (isWebPlatform as jest.Mock).mockReturnValue(false);
    (useNavigation as jest.Mock).mockReturnValue({ navigate, goBack });
    granted();
  });

  afterEach(cleanup);

  async function scan(user: ReturnType<typeof userEvent.setup>, data: string) {
    mockQueuedScan = data;
    await user.press(screen.getByLabelText('__camera__'));
  }

  it('only looks for QR codes', async () => {
    await render(<ScanScreen />);
    expect(mockCameraSettings).toEqual({ barcodeTypes: ['qr'] });
  });

  it('opens the guide for a scanned plan URL', async () => {
    const user = userEvent.setup();
    await render(<ScanScreen />);

    await scan(user, 'https://gema.app/q/3zhmodkfwk');

    expect(navigate).toHaveBeenCalledWith('EmergencyGuide', { publicId: '3zhmodkfwk' });
  });

  it('ignores repeat reads of the same code', async () => {
    // The camera fires continuously while a code is in frame; without a latch
    // this would push the guide screen dozens of times.
    const user = userEvent.setup();
    await render(<ScanScreen />);

    await scan(user, 'https://gema.app/q/3zhmodkfwk');
    await scan(user, 'https://gema.app/q/3zhmodkfwk');
    await scan(user, 'https://gema.app/q/3zhmodkfwk');

    expect(navigate).toHaveBeenCalledTimes(1);
  });

  it('says so when the code is not a GEMA plan, and keeps scanning', async () => {
    const user = userEvent.setup();
    await render(<ScanScreen />);

    await scan(user, 'WIFI:S:casa;T:WPA;P:senha;;');

    expect(screen.getByText('Esse código não é um plano da GEMA.')).toBeOnTheScreen();
    expect(navigate).not.toHaveBeenCalled();

    // Still live: the person is probably pointing at the wrong thing.
    await scan(user, 'https://gema.app/q/3zhmodkfwk');
    expect(navigate).toHaveBeenCalledWith('EmergencyGuide', { publicId: '3zhmodkfwk' });
  });

  it('offers to ask for the camera when permission was refused', async () => {
    const requestPermission = jest.fn();
    mockPermissions.mockReturnValue([{ granted: false, canAskAgain: true }, requestPermission]);
    const user = userEvent.setup();
    await render(<ScanScreen />);

    await user.press(screen.getByText('Permitir câmera'));

    expect(requestPermission).toHaveBeenCalled();
  });

  it('still lets the code be typed when permission is permanently refused', async () => {
    // A scanner that just fails is useless; the code is printed next to the QR.
    mockPermissions.mockReturnValue([{ granted: false, canAskAgain: false }, jest.fn()]);
    const user = userEvent.setup();
    await render(<ScanScreen />);

    expect(screen.queryByText('Permitir câmera')).not.toBeOnTheScreen();
    await user.type(screen.getByLabelText('Código do plano'), '3zhmodkfwk');
    await user.press(screen.getByText('Abrir guia'));

    expect(navigate).toHaveBeenCalledWith('EmergencyGuide', { publicId: '3zhmodkfwk' });
  });

  it('rejects a typed code of the wrong shape', async () => {
    mockPermissions.mockReturnValue([{ granted: false, canAskAgain: false }, jest.fn()]);
    const user = userEvent.setup();
    await render(<ScanScreen />);

    await user.type(screen.getByLabelText('Código do plano'), 'nao-existe');
    await user.press(screen.getByText('Abrir guia'));

    expect(screen.getByText(/Código inválido/)).toBeOnTheScreen();
    expect(navigate).not.toHaveBeenCalled();
  });

  it('goes straight to typing on web, where the camera scanner is not usable', async () => {
    (isWebPlatform as jest.Mock).mockReturnValue(true);
    await render(<ScanScreen />);

    expect(screen.getByText(/funciona no aplicativo do celular/)).toBeOnTheScreen();
    expect(screen.getByLabelText('Código do plano')).toBeOnTheScreen();
  });

  it('renders nothing while the permission state is still unknown', async () => {
    // Flashing a "denied" screen before the answer arrives would be a lie.
    mockPermissions.mockReturnValue([null, jest.fn()]);
    await render(<ScanScreen />);

    expect(screen.queryByText('Abrir um guia')).not.toBeOnTheScreen();
  });
});
