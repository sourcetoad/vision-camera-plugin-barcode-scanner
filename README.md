# vision-camera-plugin-barcode-scanner
_A [VisionCamera v2](https://mrousavy.com/react-native-vision-camera/) plugin that utilizes [Google ML Kit](https://developers.google.com/ml-kit) for parsing._

* This package works at present ONLY against `vision-camera` v2.
* This package works at present ONLY against `react-native-reanimated` v3.

## Installation

make sure to install both the `@sourcetoad/vision-camera-plugin-barcode-scanner` and `react-native-reanimated` plugins

**npm**
```sh
npm install @sourcetoad/vision-camera-plugin-barcode-scanner react-native-reanimated
```

**yarn**
```sh
yarn add @sourcetoad/vision-camera-plugin-barcode-scanner react-native-reanimated
```
## Setup

Edit your `babel.config.js` file to include the following
```js
plugins: [
    [
      'react-native-reanimated/plugin',
      {
        globals: ['__read_codes_via_ml_kit'],
      },
    ],
]
```

## Usage

call the `useBarcodeScanner()` hook and pass the `BarcodeScannerFormat` type as a parameter. If no parameter is passed. the camera will scan for all barcodes
> Note: Specifying the type of barcode you wish to scan allows for faster detection

> Ex. `const [barcodeData, frameProcessor] = useBarcodeScanner(BarcodeScannerFormats.QR_CODE)` only checks for QR barcodes, resulting in faster detection

#### barcode types
```ts
export enum BarcodeScannerFormats {
  All = 1, // -> scans for all barcodes
  QR_CODE = 2,
  PDF_417 = 3,
}
```

the `useBarcodeScanner()` hooks returns a state array with the `barcodeData` and the `frameProcesssor` which you pass to the  `<Camera/>` component.

Ex.
```js
  import {useBarcodeScanner, BarcodeScannerFormats} from 'vision-camera-plugin-barcode-scanner'


  const [barcodeData, frameProcessor] = useBarcodeScanner(BarcodeScannerFormats.All)

  useEffect(() => {
    if (barcodeData !== undefined) {
        // handle barcode detection
    }
  }, [barcodeData])

  if (device == null) return <LoadingView />
  return (
    <Camera
      device={device}
      isActive={true}
      style={StyleSheet.absoluteFill}
      frameProcessor={frameProcessor}
    />
  )
```


## Full usage
```js

import React, { useEffect, useState } from 'react';
import { StyleSheet, View, Text, Button } from 'react-native';
import { Camera, useCameraDevices } from 'react-native-vision-camera';
import {useBarcodeScanner, BarcodeScannerFormats} from 'vision-camera-plugin-barcode-scanner'

export default function App() {
  const [permsGranted, setPermsGranted] = useState(false)
  const [displayCamera, setDisplayCamera] = useState(false)
  const [barcodeData, frameProcessor] = useBarcodeScanner(BarcodeScannerFormats.All)

  const devices = useCameraDevices()
  const device = devices.back

  useEffect(() => {
    /**
     * add code set camera permissions
     * https://mrousavy.com/react-native-vision-camera/docs/guides#getting-permissions
    */
   setPermsGranted(true)
  })

  useEffect(() => {
    if (barcodeData !== undefined) {
      setDisplayCamera(false)
    }
  }, [barcodeData])

  if (device == null) {
    return (<></>)

  } else if (!permsGranted) {
    return (<></>)
  } else if (!displayCamera) {
    return (
      <View style={styles.container}>
        <Button title='Open Camera' onPress={() => setDisplayCamera(true)}></Button>
        <Text style={styles.resultsText}>Result: {barcodeData?.rawValue}</Text>
      </View>
    )
  } else {
    return (
      <Camera
        device={device}
        isActive={true}
        style={StyleSheet.absoluteFill}
        frameProcessor={frameProcessor}
      />
    )
  }
}
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
