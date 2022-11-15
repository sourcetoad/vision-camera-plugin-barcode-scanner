import {useBarcodeScanner} from './scanner'
import type {Frame} from 'react-native-vision-camera'

export enum BarcodeScannerFormats {
  All = 1,
  QR_CODE = 2,
  PDF_417 = 3,
}

type CornerPoint = {
  x: number
  y: number
}

export type FrameProcessor = (frame: Frame) => void
export type BarcodeData = {
  rawValue: string
  displayValue: string
  cornerPoints: CornerPoint[]
}
export {useBarcodeScanner}