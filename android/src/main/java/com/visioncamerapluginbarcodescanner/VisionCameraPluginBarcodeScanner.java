package com.visioncamerapluginbarcodescanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
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

import java.nio.ByteBuffer;
import java.util.List;
import android.graphics.Point;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;

public class VisionCameraPluginBarcodeScanner extends FrameProcessorPlugin {

  private Context context;

  public void setContext(Context context) {
    this.context = context;
  }

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

  // https://stackoverflow.com/questions/47498843/incorrect-image-converting-yuv-420-888-into-bitmaps-under-android-camera2/47601824#47601824
  private ByteBuffer imageToByteBuffer(final Image image) {
    final Rect crop = image.getCropRect();
    final int width = crop.width();
    final int height = crop.height();

    final Image.Plane[] planes = image.getPlanes();
    final byte[] rowData = new byte[planes[0].getRowStride()];
    final int bufferSize = width * height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8;
    final ByteBuffer output = ByteBuffer.allocateDirect(bufferSize);

    int channelOffset = 0;
    int outputStride = 0;

    for (int planeIndex = 0; planeIndex < 3; planeIndex++) {
      if (planeIndex == 0) {
        channelOffset = 0;
        outputStride = 1;
      } else if (planeIndex == 1) {
        channelOffset = width * height + 1;
        outputStride = 2;
      } else if (planeIndex == 2) {
        channelOffset = width * height;
        outputStride = 2;
      }

      final ByteBuffer buffer = planes[planeIndex].getBuffer();
      final int rowStride = planes[planeIndex].getRowStride();
      final int pixelStride = planes[planeIndex].getPixelStride();

      final int shift = (planeIndex == 0) ? 0 : 1;
      final int widthShifted = width >> shift;
      final int heightShifted = height >> shift;

      buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));

      for (int row = 0; row < heightShifted; row++) {
        final int length;

        if (pixelStride == 1 && outputStride == 1) {
          length = widthShifted;
          buffer.get(output.array(), channelOffset, length);
          channelOffset += length;
        } else {
          length = (widthShifted - 1) * pixelStride + 1;
          buffer.get(rowData, 0, length);

          for (int col = 0; col < widthShifted; col++) {
            output.array()[channelOffset] = rowData[col * pixelStride];
            channelOffset += outputStride;
          }
        }

        if (row < heightShifted - 1) {
          buffer.position(buffer.position() + rowStride - length);
        }
      }
    }

    return output;
  }

  private Bitmap convertImageToBitmap(Image image, Context context) {
    if (image == null) return null;
    Allocation allocationYuv;
    Allocation allocationRgb;
    RenderScript rs;
    final ByteBuffer yuvBytes = imageToByteBuffer(image);

    // Convert YUV to RGB
    rs = RenderScript.create(context);

    Bitmap bitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
    allocationRgb = Allocation.createFromBitmap(rs, bitmap);

    allocationYuv = Allocation.createSized(rs, Element.U8(rs), yuvBytes.array().length);
    allocationYuv.copyFrom(yuvBytes.array());

    ScriptIntrinsicYuvToRGB scriptYuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
    scriptYuvToRgb.setInput(allocationYuv);
    scriptYuvToRgb.forEach(allocationRgb);

    allocationRgb.copyTo(bitmap);
    return bitmap;
  }

  public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
    int width = image.getWidth();
    int height = image.getHeight();

    float bitmapRatio = (float) width / (float) height;
    if (bitmapRatio > 1) {
      width = maxSize;
      height = (int) (width / bitmapRatio);
    } else {
      height = maxSize;
      width = (int) (height * bitmapRatio);
    }
    return Bitmap.createScaledBitmap(image, width, height, true);
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
      Bitmap bitmapImage = convertImageToBitmap(imageProxy.getImage(), this.context);
      if (bitmapImage != null) {
        InputImage image = InputImage.fromBitmap(bitmapImage, imageProxy.getImageInfo().getRotationDegrees());
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
