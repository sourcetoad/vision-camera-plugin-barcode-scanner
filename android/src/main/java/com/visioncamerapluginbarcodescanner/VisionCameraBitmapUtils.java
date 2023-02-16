package com.visioncamerapluginbarcodescanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;

import java.nio.ByteBuffer;

public class VisionCameraBitmapUtils {
  private Bitmap bitmap;

  public VisionCameraBitmapUtils(Image image, Context context) {
    this.bitmap = convertImageToBitmap(image, context);
  }

  public Bitmap getBitmap() {
    return this.bitmap;
  }
  public int getHeight() {
    return this.bitmap.getHeight();
  }
  public int getWidth() {
    return this.bitmap.getWidth();
  }

  public Bitmap setBitmapScale(int width, int height)  {
    this.bitmap = Bitmap.createScaledBitmap(this.bitmap, width, height, true);
    return this.bitmap;
  }


  private Bitmap convertImageToBitmap(Image image, Context context) {
    if (image == null) {
      return null;
    };
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

  private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
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
    return Bitmap.createScaledBitmap(image, height, width , true);
  }
}
