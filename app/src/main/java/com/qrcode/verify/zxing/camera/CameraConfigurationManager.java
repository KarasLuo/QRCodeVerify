/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qrcode.verify.zxing.camera;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.google.zxing.client.android.camera.CameraConfigurationUtils;
import com.qrcode.verify.zxing.camera.open.CameraFacing;
import com.qrcode.verify.zxing.camera.open.OpenCamera;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * A class which deals with reading, parsing, and setting the camera parameters which are used to
 * configure the camera hardware.
 */
@SuppressWarnings("deprecation") // camera APIs
final class CameraConfigurationManager {

  private static final String TAG = "CameraConfiguration";

  private final Context context;
  private int cwNeededRotation;
  private int cwRotationFromDisplayToCamera;
  private Point screenResolution;
  private Point cameraResolution;
  private Point bestPreviewSize;
  private Point previewSizeOnScreen;

  CameraConfigurationManager(Context context) {
    this.context = context;
  }

  /**
   * Reads, one time, values from the camera that are needed by the app.
   */
  void initFromCameraParameters(OpenCamera camera) {
    Camera.Parameters parameters = camera.getCamera().getParameters();
    WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    Display display = manager.getDefaultDisplay();

    int displayRotation = display.getRotation();
    int cwRotationFromNaturalToDisplay;
    switch (displayRotation) {
      case Surface.ROTATION_0:
        cwRotationFromNaturalToDisplay = 0;
        break;
      case Surface.ROTATION_90:
        cwRotationFromNaturalToDisplay = 90;
        break;
      case Surface.ROTATION_180:
        cwRotationFromNaturalToDisplay = 180;
        break;
      case Surface.ROTATION_270:
        cwRotationFromNaturalToDisplay = 270;
        break;
      default:
        // Have seen this return incorrect values like -90
        if (displayRotation % 90 == 0) {
          cwRotationFromNaturalToDisplay = (360 + displayRotation) % 360;
        } else {
          throw new IllegalArgumentException("Bad rotation: " + displayRotation);
        }
    }
    Log.i(TAG, "Display at: " + cwRotationFromNaturalToDisplay);

    int cwRotationFromNaturalToCamera = camera.getOrientation();
    Log.i(TAG, "Camera at: " + cwRotationFromNaturalToCamera);

    // Still not 100% sure about this. But acts like we need to flip this:
    if (camera.getFacing() == CameraFacing.FRONT) {
      cwRotationFromNaturalToCamera = (360 - cwRotationFromNaturalToCamera) % 360;
      Log.i(TAG, "Front camera overriden to: " + cwRotationFromNaturalToCamera);
    }

//    cwRotationFromDisplayToCamera =cwRotationFromNaturalToDisplay;
    cwRotationFromDisplayToCamera =
            (360 + cwRotationFromNaturalToCamera - cwRotationFromNaturalToDisplay) % 360;

    Log.i(TAG, "Final display orientation: " + cwRotationFromDisplayToCamera);
    if (camera.getFacing() == CameraFacing.FRONT) {
      Log.i(TAG, "Compensating rotation for front camera");
      cwNeededRotation = (360 - cwRotationFromDisplayToCamera) % 360;
    } else {
      cwNeededRotation = cwRotationFromDisplayToCamera;
    }
    Log.i(TAG, "Clockwise rotation from display to camera: " + cwNeededRotation);

    Point theScreenResolution = new Point();
    display.getSize(theScreenResolution);
    screenResolution = theScreenResolution;
    Log.i(TAG, "Screen resolution in current orientation: " + screenResolution);

    DisplayMetrics dm = context.getResources().getDisplayMetrics();
    float density = dm.density;
    int widthByResources = dm.widthPixels;
    int heightByResources = dm.heightPixels;
    cameraResolution = findBestPreviewSizeValue(parameters, screenResolution);

//    cameraResolution=new Point(widthByResources,heightByResources);
    Log.i(TAG, "Camera resolution: " + cameraResolution);
    bestPreviewSize = findBestPreviewSizeValue(parameters, screenResolution);
//    bestPreviewSize=new Point(widthByResources,heightByResources);
    Log.i(TAG, "Best available preview size: " + bestPreviewSize);

    boolean isScreenPortrait = screenResolution.x < screenResolution.y;
    boolean isPreviewSizePortrait = bestPreviewSize.x < bestPreviewSize.y;

    Log.e(TAG,"screenResolution.x="+screenResolution.x+",screenResolution.y="+screenResolution.y);
    Log.e(TAG,"bestPreviewSize.x="+bestPreviewSize.x+",bestPreviewSize.y="+bestPreviewSize.y);

    if (isScreenPortrait == isPreviewSizePortrait) {
      previewSizeOnScreen = bestPreviewSize;
    } else {
      previewSizeOnScreen = new Point(bestPreviewSize.y, bestPreviewSize.x);
    }
    Log.i(TAG, "Preview size on screen: " + previewSizeOnScreen);
  }

    private static Point findBestPreviewSizeValue(Camera.Parameters parameters, Point screenResolution) {
        List<Camera.Size> rawSupportedSizes = parameters.getSupportedPreviewSizes();
        if (rawSupportedSizes == null) {
            Log.w("CameraConfiguration", "Device returned no supported preview sizes; using default");
            Camera.Size defaultSize = parameters.getPreviewSize();
            if (defaultSize == null) {
                throw new IllegalStateException("Parameters contained no preview size!");
            } else {
                return new Point(defaultSize.width, defaultSize.height);
            }
        } else {
            List<Camera.Size> supportedPreviewSizes = new ArrayList(rawSupportedSizes);
            Collections.sort(supportedPreviewSizes, new Comparator<Camera.Size>() {
                public int compare(Camera.Size a, Camera.Size b) {
                    int aPixels = a.height * a.width;
                    int bPixels = b.height * b.width;
                    if (bPixels < aPixels) {
                        return -1;
                    } else {
                        return bPixels > aPixels ? 1 : 0;
                    }
                }
            });
            if (Log.isLoggable("CameraConfiguration", Log.INFO)) {
                StringBuilder previewSizesString = new StringBuilder();
                Iterator var5 = supportedPreviewSizes.iterator();

                while(var5.hasNext()) {
                    Camera.Size supportedPreviewSize = (Camera.Size)var5.next();
                    previewSizesString.append(supportedPreviewSize.width).append('x').append(supportedPreviewSize.height).append(' ');
                }

                Log.i("CameraConfiguration", "Supported preview sizes: " + previewSizesString);
            }

            double screenAspectRatio = (double)screenResolution.x / (double)screenResolution.y;
            Iterator it = supportedPreviewSizes.iterator();

            Camera.Size defaultPreview;
            while(it.hasNext()) {
                defaultPreview = (Camera.Size)it.next();
                int realWidth = defaultPreview.width;
                int realHeight = defaultPreview.height;
                if (realWidth * realHeight < 153600) {
                    it.remove();
                } else {
                    boolean isCandidatePortrait = realWidth < realHeight;
                    int maybeFlippedWidth = isCandidatePortrait ?  realWidth:realHeight ;
                    int maybeFlippedHeight = isCandidatePortrait ? realHeight : realWidth;
                    double aspectRatio = (double)maybeFlippedWidth / (double)maybeFlippedHeight;
                    double distortion = Math.abs(aspectRatio - screenAspectRatio);
                    if (distortion > 0.15D) {
                        it.remove();
                    } else if (maybeFlippedWidth == screenResolution.x && maybeFlippedHeight == screenResolution.y) {
                        Point exactPoint = new Point(realWidth, realHeight);
                        Log.i("CameraConfiguration", "Found preview size exactly matching screen size: " + exactPoint);
                        return exactPoint;
                    }
                }
            }

            final int srX=screenResolution.x;
            if (!supportedPreviewSizes.isEmpty()) {
                Collections.sort(supportedPreviewSizes, new Comparator<Camera.Size>() {
                    @Override
                    public int compare(Camera.Size o1, Camera.Size o2) {
                        int delta1 = Math.abs(o1.height-srX);
                        int delta2 = Math.abs(o2.height-srX);
                        return delta1 - delta2;
                    }
                });
                Camera.Size bestPreview = supportedPreviewSizes.get(0);
                Point bestSize = new Point(bestPreview.width, bestPreview.height);
                return bestSize;
            }else {
                Point defaultSize;
                if (!supportedPreviewSizes.isEmpty()) {
                    defaultPreview = (Camera.Size)supportedPreviewSizes.get(0);
                    defaultSize = new Point(defaultPreview.width, defaultPreview.height);
                    Log.i("CameraConfiguration", "Using largest suitable preview size: " + defaultSize);
                    return defaultSize;
                } else {
                    defaultPreview = parameters.getPreviewSize();
                    if (defaultPreview == null) {
                        throw new IllegalStateException("Parameters contained no preview size!");
                    } else {
                        defaultSize = new Point(defaultPreview.width, defaultPreview.height);
                        Log.i("CameraConfiguration", "No suitable preview sizes, using default: " + defaultSize);
                        return defaultSize;
                    }
                }
            }
        }
    }


  void setDesiredCameraParameters(OpenCamera camera, boolean safeMode) {

    Camera theCamera = camera.getCamera();
    Camera.Parameters parameters = theCamera.getParameters();

    if (parameters == null) {
      Log.w(TAG, "Device error: no camera parameters are available. Proceeding without configuration.");
      return;
    }

    Log.i(TAG, "Initial camera parameters: " + parameters.flatten());

    if (safeMode) {
      Log.w(TAG, "In camera config safe mode -- most settings will not be honored");
    }

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

    initializeTorch(parameters, prefs, safeMode);

    CameraConfigurationUtils.setFocus(
        parameters,
            true,true,//控制对焦
        safeMode);

    if (!safeMode) {
//      if (false) {//控制反转
//        CameraConfigurationUtils.setInvertColor(parameters);
//      }

//      if (! true) {//控制条形码扫描模式
//        CameraConfigurationUtils.setBarcodeSceneMode(parameters);
//      }

//      if (!true) {//控制测光？
//        CameraConfigurationUtils.setVideoStabilization(parameters);
//        CameraConfigurationUtils.setFocusArea(parameters);
//        CameraConfigurationUtils.setMetering(parameters);
//      }

      //SetRecordingHint to true also a workaround for low framerate on Nexus 4
      //https://stackoverflow.com/questions/14131900/extreme-camera-lag-on-nexus-4
      parameters.setRecordingHint(true);

    }

    parameters.setPreviewSize(bestPreviewSize.x, bestPreviewSize.y);

    theCamera.setParameters(parameters);

    theCamera.setDisplayOrientation(cwRotationFromDisplayToCamera);

    Camera.Parameters afterParameters = theCamera.getParameters();
    Camera.Size afterSize = afterParameters.getPreviewSize();
    if (afterSize != null && (bestPreviewSize.x != afterSize.width || bestPreviewSize.y != afterSize.height)) {
      Log.w(TAG, "Camera said it supported preview size " + bestPreviewSize.x + 'x' + bestPreviewSize.y +
          ", but after setting it, preview size is " + afterSize.width + 'x' + afterSize.height);
      bestPreviewSize.x = afterSize.width;
      bestPreviewSize.y = afterSize.height;
    }
  }


  Point getBestPreviewSize() {
    return bestPreviewSize;
  }

  Point getPreviewSizeOnScreen() {
    return previewSizeOnScreen;
  }

  Point getCameraResolution() {
    return cameraResolution;
  }

  Point getScreenResolution() {
    return screenResolution;
  }

  int getCWNeededRotation() {
    return cwNeededRotation;
  }

  boolean getTorchState(Camera camera) {
    if (camera != null) {
      Camera.Parameters parameters = camera.getParameters();
      if (parameters != null) {
        String flashMode = parameters.getFlashMode();
        return
            Camera.Parameters.FLASH_MODE_ON.equals(flashMode) ||
            Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode);
      }
    }
    return false;
  }

  void setTorch(Camera camera, boolean newSetting) {
    Camera.Parameters parameters = camera.getParameters();
    doSetTorch(parameters, newSetting, false);
    camera.setParameters(parameters);
  }

  private void initializeTorch(Camera.Parameters parameters, SharedPreferences prefs, boolean safeMode) {
    boolean currentSetting = FrontLightMode.readPref(prefs) == FrontLightMode.ON;
    doSetTorch(parameters, currentSetting, safeMode);
  }

  private void doSetTorch(Camera.Parameters parameters, boolean newSetting, boolean safeMode) {
    CameraConfigurationUtils.setTorch(parameters, newSetting);
//    if (!safeMode && ! true) {//控制曝光
//      CameraConfigurationUtils.setBestExposure(parameters, newSetting);
//    }
  }

}
