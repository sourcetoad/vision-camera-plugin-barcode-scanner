import MLKitVision
import MLKitBarcodeScanning

@objc(VisionCameraPluginBarcodeScanner)
class VisionCameraPluginBarcodeScanner: NSObject, FrameProcessorPluginBase {

  private static func getScannerOptions(scannerFormat: Int?) -> BarcodeScannerOptions {
    var barcodeOptions: BarcodeScannerOptions
    switch scannerFormat {
        case 2:
            barcodeOptions = BarcodeScannerOptions(formats: .qrCode)
        case 3:
            barcodeOptions = BarcodeScannerOptions(formats: .PDF417)
        default:
            barcodeOptions = BarcodeScannerOptions(formats: .all)
        }
    return barcodeOptions
  }
    
  private static func getScannerResults(image: VisionImage, barcodeScanner: BarcodeScanner) -> Barcode? {
    do {
        let barcodes = try barcodeScanner.results(in: image)
        if (barcodes.count < 1) {
            return nil
        }
        // we only want to return the first instance
        // since we dont care about scanning mulitple barcodes at the same time
        return barcodes[0]
    } catch {
        return nil
    }
  }
    
  private static func mapCGPoints(cornerPoints: [CGPoint]?) -> [[String: Double]] {
    guard let cornerPoints = cornerPoints else {
        return []
    }
    return cornerPoints.map {return ["x": $0.x, "y": $0.y]}
  }
    
  private static func mapBarcodeData(barcodeResults: Barcode) -> Any? {
    let barcodeData: [String: Any?] = [
        "rawValue": barcodeResults.rawValue,
        "displayValue": barcodeResults.displayValue,
        "cornerPoints": mapCGPoints(cornerPoints: barcodeResults.cornerPoints as? [CGPoint]),
    ]
    return barcodeData
  }
    
  @objc
  public static func callback(_ frame: Frame!, withArgs args: [Any]!) -> Any! {
    let image = VisionImage(buffer: frame.buffer)
    image.orientation = frame.orientation
    let format = args[0] as? Int
    let barcodeOptions = getScannerOptions(scannerFormat: format)
    guard let barcodeResults = getScannerResults(image: image, barcodeScanner: BarcodeScanner.barcodeScanner(options: barcodeOptions)) else {
          return nil
    }
    let barcodeData = mapBarcodeData(barcodeResults: barcodeResults)
    return barcodeData
  }
}
