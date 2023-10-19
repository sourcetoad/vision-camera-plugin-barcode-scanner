import React, { useEffect, useState } from 'react';
import { StyleSheet, View, Text, Button } from 'react-native';
import { Camera, useCameraDevices } from 'react-native-vision-camera';
import { useBarcodeScanner, BarcodeScannerFormats } from '../../src/index';

export default function App() {
  // state
  const [permsGranted, setPermsGranted] = useState(false);
  const [displayCamera, setDisplayCamera] = useState(false);
  const [barcodeData, frameProcessor] = useBarcodeScanner(
    BarcodeScannerFormats.All
  );
  // hooks
  const devices = useCameraDevices();
  const device = devices.back;

  useEffect(() => {
    (async () => {
      const cameraStatusPermission = await Camera.getCameraPermissionStatus();

      if (cameraStatusPermission === 'authorized') {
        setPermsGranted(true);
      } else {
        const cameraPermission = await Camera.requestCameraPermission();
        if (cameraPermission !== 'authorized') {
          // link to open settings
        } else {
          setPermsGranted(true);
        }
      }
    })();
  }, [permsGranted]);

  useEffect(() => {
    if (barcodeData !== undefined) {
      setDisplayCamera(false);
    }
  }, [barcodeData]);

  if (device == null) {
    return <></>;
  } else if (!permsGranted) {
    return <></>;
  } else if (!displayCamera) {
    return (
      <View style={styles.container}>
        <Button title="Open Camera" onPress={() => setDisplayCamera(true)} />
        <Text style={styles.resultsText}>Result: {barcodeData?.rawValue}</Text>
      </View>
    );
  } else {
    return (
      <Camera
        device={device}
        isActive={true}
        style={StyleSheet.absoluteFill}
        frameProcessor={frameProcessor}
        frameProcessorFps={1}
      />
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'white',
  },
  resultsText: {
    fontSize: 18,
    marginTop: 30,
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
