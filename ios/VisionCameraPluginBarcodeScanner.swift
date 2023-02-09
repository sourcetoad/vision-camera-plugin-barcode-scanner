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
        let filter = CIFilter(name: "CILanczosScaleTransform")!

        filter.setValue(image, forKey: kCIInputImageKey)
        filter.setValue(1.1, forKey: kCIInputScaleKey)
        filter.setValue(1.0, forKey: kCIInputAspectRatioKey)
        guard let scaledImage = filter.outputImage else {
            return nil
        }
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
        
        // Print out errors
        if osStatus == kCMSampleBufferError_AllocationFailed {
          print("osStatus == kCMSampleBufferError_AllocationFailed")
        }
        if osStatus == kCMSampleBufferError_RequiredParameterMissing {
          print("osStatus == kCMSampleBufferError_RequiredParameterMissing")
        }
        if osStatus == kCMSampleBufferError_AlreadyHasDataBuffer {
          print("osStatus == kCMSampleBufferError_AlreadyHasDataBuffer")
        }
        if osStatus == kCMSampleBufferError_BufferNotReady {
          print("osStatus == kCMSampleBufferError_BufferNotReady")
        }
        if osStatus == kCMSampleBufferError_SampleIndexOutOfRange {
          print("osStatus == kCMSampleBufferError_SampleIndexOutOfRange")
        }
        if osStatus == kCMSampleBufferError_BufferHasNoSampleSizes {
          print("osStatus == kCMSampleBufferError_BufferHasNoSampleSizes")
        }
        if osStatus == kCMSampleBufferError_BufferHasNoSampleTimingInfo {
          print("osStatus == kCMSampleBufferError_BufferHasNoSampleTimingInfo")
        }
        if osStatus == kCMSampleBufferError_ArrayTooSmall {
          print("osStatus == kCMSampleBufferError_ArrayTooSmall")
        }
        if osStatus == kCMSampleBufferError_InvalidEntryCount {
          print("osStatus == kCMSampleBufferError_InvalidEntryCount")
        }
        if osStatus == kCMSampleBufferError_CannotSubdivide {
          print("osStatus == kCMSampleBufferError_CannotSubdivide")
        }
        if osStatus == kCMSampleBufferError_SampleTimingInfoInvalid {
          print("osStatus == kCMSampleBufferError_SampleTimingInfoInvalid")
        }
        if osStatus == kCMSampleBufferError_InvalidMediaTypeForOperation {
          print("osStatus == kCMSampleBufferError_InvalidMediaTypeForOperation")
        }
        if osStatus == kCMSampleBufferError_InvalidSampleData {
          print("osStatus == kCMSampleBufferError_InvalidSampleData")
        }
        if osStatus == kCMSampleBufferError_InvalidMediaFormat {
          print("osStatus == kCMSampleBufferError_InvalidMediaFormat")
        }
        if osStatus == kCMSampleBufferError_Invalidated {
          print("osStatus == kCMSampleBufferError_Invalidated")
        }
        if osStatus == kCMSampleBufferError_DataFailed {
          print("osStatus == kCMSampleBufferError_DataFailed")
        }
        if osStatus == kCMSampleBufferError_DataCanceled {
          print("osStatus == kCMSampleBufferError_DataCanceled")
        }
        
        guard let buffer = sampleBuffer else {
          print("Cannot create sample buffer")
          return nil
        }
        
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
      
    guard let barcodeResults = getScannerResults(image: image, barcodeScanner: BarcodeScanner.barcodeScanner(options: barcodeOptions)) else {
          return nil
    }

    let barcodeData = mapBarcodeData(barcodeResults: barcodeResults)
    return barcodeData
  }
}
