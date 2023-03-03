package com.visioncamerapluginbarcodescanner;

import android.media.Image;

import androidx.camera.core.ImageProxy;

import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.mrousavy.camera.frameprocessor.FrameProcessorPlugin;
import java.util.List;
import android.graphics.Point;

public class VisionCameraPluginBarcodeScanner extends FrameProcessorPlugin {
  private BarcodeScannerOptions getScannerOptions(int format) {
    int barcodeFormat;
    switch (format) {
      case 2:
        barcodeFormat = Barcode.FORMAT_QR_CODE;
        break;
      case 3:
        barcodeFormat = Barcode.FORMAT_PDF417;
        break;
      default:
        barcodeFormat = Barcode.FORMAT_ALL_FORMATS;
    }
    BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
        .setBarcodeFormats(barcodeFormat).build();

    return options;
  }

  private Barcode getScannerResults(InputImage image, BarcodeScanner scanner) {
    try {
      Task<List<Barcode>> scan = scanner.process(image);
      List<Barcode> results = Tasks.await(scan);
      if (results.size() < 1) {
        return null;
      }
      // we only want to return the first instance
      // since we don't care about scanning multiple barcodes at the same time
      return results.get(0);
    } catch (Exception e) {
      return null;
    }
  }

  private WritableNativeArray mapCornerPoints(Point[] cornerPoints) {
    WritableNativeArray pointArray = new WritableNativeArray();
    for (Point point : cornerPoints) {
      WritableNativeMap pointMap = new WritableNativeMap();
      pointMap.putInt("x", point.x);
      pointMap.putInt("y", point.y);
      pointArray.pushMap(pointMap);
    }
    return pointArray;
  }

  private WritableNativeMap mapBarcodeData(Barcode barcodeResults) {
    WritableNativeMap map = new WritableNativeMap();
    map.putString("rawValue", barcodeResults.getRawValue());
    map.putString("displayValue", barcodeResults.getDisplayValue());
    map.putArray("cornerPoints", mapCornerPoints(barcodeResults.getCornerPoints()));

    return map;
  }

  @Override
  @androidx.camera.core.ExperimentalGetImage
  public Object callback(ImageProxy imageProxy, Object[] params) {
    try {
      Double scannerFormat = (Double) params[0];
      BarcodeScannerOptions scannerOptions = getScannerOptions(scannerFormat.intValue());
      BarcodeScanner scannerClient = BarcodeScanning.getClient(scannerOptions);
      Image mediaImage = imageProxy.getImage();
      if (mediaImage != null) {
        InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
        Barcode barcodeResults = getScannerResults(image, scannerClient);
        if (barcodeResults != null) {
          return mapBarcodeData(barcodeResults);
        }
        return null;
      }
      return null;
    } catch (Exception e) {
      return null;
    }
  }

  public VisionCameraPluginBarcodeScanner() {
    super("scanQRCodes");
  }
}
