package com.visioncamerapluginbarcodescanner;

import androidx.annotation.NonNull;
import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;
import com.mrousavy.camera.frameprocessor.FrameProcessorPlugin;

import java.util.Collections;
import java.util.List;


public class VisionCameraPluginBarcodeScannerPackage implements ReactPackage {
  @NonNull
  @Override
  public List<NativeModule> createNativeModules(@NonNull ReactApplicationContext reactContext) {
    VisionCameraPluginBarcodeScanner VisionCamera = new VisionCameraPluginBarcodeScanner();
    VisionCamera.setContext(reactContext);
    FrameProcessorPlugin.register(VisionCamera);
    return Collections.emptyList();
  }

  @NonNull
  @Override
  public List<ViewManager> createViewManagers(@NonNull ReactApplicationContext reactContext) {
    return Collections.emptyList();
  }
}
