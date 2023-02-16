import MLKitVision
import MLKitBarcodeScanning
import CoreGraphics

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
    
    private static func getImageSize(sampleBuffer: CMSampleBuffer) -> CGSize {
        let imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer);
        let imageWidth  = CVPixelBufferGetWidth(imageBuffer!);
        let imageHeight = CVPixelBufferGetHeight(imageBuffer!);
        return CGSizeMake(CGFloat(imageWidth), CGFloat(imageHeight))
    }

    private static func scaleImage(sampleBuffer: CMSampleBuffer) -> CMSampleBuffer? {
        let imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer)!
        let image = CIImage(cvPixelBuffer: imageBuffer)
        
        let scaleFilter = CIFilter(name: "CILanczosScaleTransform")
        scaleFilter?.setValue(image, forKey: kCIInputImageKey)
        scaleFilter?.setValue([1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7].randomElement(), forKey: kCIInputScaleKey)
        scaleFilter?.setValue(1.0, forKey: kCIInputAspectRatioKey)
        guard let scaledImage = scaleFilter?.outputImage else { return nil }

        let context = CIContext()
        context.render(scaledImage, to: imageBuffer)
        let sampleBuffer: CMSampleBuffer? = createSampleBufferFrom(pixelBuffer: imageBuffer)
        return sampleBuffer
    }

    private static func createSampleBufferFrom(pixelBuffer: CVPixelBuffer) -> CMSampleBuffer? {
        var sampleBuffer: CMSampleBuffer?
        var timimgInfo  = CMSampleTimingInfo()
        var formatDescription: CMFormatDescription? = nil
        
        CMVideoFormatDescriptionCreateForImageBuffer(allocator: kCFAllocatorDefault, imageBuffer: pixelBuffer, formatDescriptionOut: &formatDescription)
        
        let osStatus = CMSampleBufferCreateReadyWithImageBuffer(
          allocator: kCFAllocatorDefault,
          imageBuffer: pixelBuffer,
          formatDescription: formatDescription!,
          sampleTiming: &timimgInfo,
          sampleBufferOut: &sampleBuffer
        )
        
        guard let buffer = sampleBuffer else { return nil }
        return buffer
    }
     
    @objc
    public static func callback(_ frame: Frame!, withArgs args: [Any]!) -> Any! {
        guard let scaledImage = scaleImage(sampleBuffer: frame.buffer) else {
            return nil
        }
      
        let image = VisionImage(buffer: scaledImage)
        let bufferSize = getImageSize(sampleBuffer: scaledImage)
        image.orientation = frame.orientation
        let format = args[0] as? Int
        let barcodeOptions = getScannerOptions(scannerFormat: format)
        
        guard let barcodeResults = getScannerResults(image: VisionImage(buffer: frame.buffer), barcodeScanner: BarcodeScanner.barcodeScanner(options: barcodeOptions)) else { return nil }

        let barcodeData = mapBarcodeData(barcodeResults: barcodeResults)
        return barcodeData
    }
}
