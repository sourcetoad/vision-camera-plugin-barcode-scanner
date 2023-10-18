import { useState, useCallback, DependencyList } from 'react';
import { runOnJS } from 'react-native-reanimated';
import type { Frame } from 'react-native-vision-camera';
import type { BarcodeScannerFormats, BarcodeData, FrameProcessor } from './';

function useFrameProcessor(
  frameProcessor: FrameProcessor,
  dependencies: DependencyList
): FrameProcessor {
  return useCallback((frame: Frame) => {
    'worklet';

    frameProcessor(frame);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, dependencies);
}

const useBarcodeScanner = (
  format: BarcodeScannerFormats
): [BarcodeData | undefined, FrameProcessor] => {
  const [barcodeData, setBarcodeData] = useState<BarcodeData>();

  const frameProcessor = useFrameProcessor((frame: Frame) => {
    'worklet';

    if (!_WORKLET) {
      throw new Error('Reanimated failed to load properly.');
    }

    // @ts-ignore
    const data = __scanQRCodes(frame, format || 1);

    /**
     * https://mrousavy.com/react-native-vision-camera/docs/guides/frame-processors-plugins-overview#types
     * swift/java - nullable value can convert to either undefined or null in js. '
     * so we conform it to undefined
     */
    data == null
      ? runOnJS(setBarcodeData)(undefined)
      : runOnJS(setBarcodeData)(data);
  }, []);

  return [barcodeData, frameProcessor];
};

export { useBarcodeScanner };
