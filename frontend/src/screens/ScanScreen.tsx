import { useCallback, useRef, useState } from 'react';
import { ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { CameraView, useCameraPermissions, type BarcodeScanningResult } from 'expo-camera';
import { Button } from '../components/Button';
import { Card } from '../components/Card';
import { Input } from '../components/Input';
import { FormError } from '../components/ScreenState';
import { isWebPlatform } from '../lib/platform';
import { extractPublicId } from '../lib/scanTarget';
import type { HomeStackParamList } from '../navigation/types';

export function ScanScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<HomeStackParamList>>();
  const [permission, requestPermission] = useCameraPermissions();
  const [error, setError] = useState<string | null>(null);
  const [manualCode, setManualCode] = useState('');
  // The camera fires this repeatedly while a code is in frame; without a latch
  // it would push the guide screen dozens of times.
  const handled = useRef(false);

  const open = useCallback(
    (publicId: string) => {
      handled.current = true;
      navigation.navigate('EmergencyGuide', { publicId });
    },
    [navigation],
  );

  const onBarcodeScanned = useCallback(
    ({ data }: BarcodeScanningResult) => {
      if (handled.current) return;
      const publicId = extractPublicId(data);
      if (!publicId) {
        // Keep the camera live: the person is probably pointing at the wrong
        // thing and will try again in a second.
        setError('Esse código não é um plano da GEMA.');
        return;
      }
      setError(null);
      open(publicId);
    },
    [open],
  );

  function submitManualCode() {
    const publicId = extractPublicId(manualCode);
    if (!publicId) {
      setError('Código inválido. Confira as 10 letras e números impressos no cartão.');
      return;
    }
    setError(null);
    open(publicId);
  }

  /** Typing the code is the way through when the camera is unavailable or refused. */
  const manualEntry = (
    <Card className="p-6">
      <Text className="font-figtreeExtrabold text-[18px] text-green-deep">Digite o código</Text>
      <Text className="mt-2 font-figtree text-[14px] leading-[1.5] text-text-muted">
        O código de 10 letras e números vem impresso junto do QR.
      </Text>
      <View className="mt-4 gap-4">
        <Input
          label="Código do plano"
          value={manualCode}
          onChangeText={setManualCode}
          placeholder="3zhmodkfwk"
          autoCapitalize="none"
        />
        <FormError message={error} />
        <Button onPress={submitManualCode}>Abrir guia</Button>
      </View>
    </Card>
  );

  // Permission state is still being read. Nothing useful to show yet, and
  // flashing a "denied" screen here would be a lie.
  if (!permission) {
    return <SafeAreaView className="flex-1 bg-cream" edges={['bottom']} />;
  }

  // expo-camera has no web implementation of the barcode scanner worth relying
  // on here, so the web build goes straight to typing the code rather than
  // showing a camera that never resolves anything.
  const cameraUnavailable = isWebPlatform();

  if (cameraUnavailable || !permission.granted) {
    return (
      <SafeAreaView className="flex-1 bg-cream" edges={['bottom']}>
        <ScrollView contentContainerClassName="flex-grow justify-center px-7 py-7" keyboardShouldPersistTaps="handled">
          <Text className="font-figtreeBold text-[12px] uppercase tracking-[0.14em] text-green-primary">Escanear</Text>
          <Text className="mt-2.5 font-figtreeExtrabold text-[26px] tracking-[-0.02em] text-green-deep">
            Abrir um guia
          </Text>
          <Text className="mt-2.5 font-figtree text-[14.5px] leading-[1.5] text-text-muted">
            {cameraUnavailable
              ? 'A leitura por câmera funciona no aplicativo do celular. Aqui, digite o código.'
              : 'Precisamos da câmera para ler o QR. Você também pode digitar o código.'}
          </Text>

          {!cameraUnavailable && permission.canAskAgain ? (
            <Button onPress={() => void requestPermission()} className="mt-5 w-full">
              Permitir câmera
            </Button>
          ) : null}

          <View className="mt-5">{manualEntry}</View>
        </ScrollView>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView className="flex-1 bg-cream" edges={['bottom']}>
      <View className="flex-1">
        <CameraView
          style={{ flex: 1 }}
          facing="back"
          barcodeScannerSettings={{ barcodeTypes: ['qr'] }}
          onBarcodeScanned={onBarcodeScanned}
        />
        <View className="absolute inset-x-0 top-0 px-7 pt-6">
          <Text className="text-center font-figtreeBold text-[15px] text-white">
            Aponte para o QR do plano
          </Text>
        </View>
      </View>

      <View className="gap-3 px-7 py-5">
        <FormError message={error} />
        <Button variant="secondary" onPress={() => navigation.goBack()} className="w-full">
          Cancelar
        </Button>
      </View>
    </SafeAreaView>
  );
}
