/*
 * Copyright (C) 2008 ZXing authors
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

package com.google.zxing.client.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.camera.CameraManager;

import java.util.ArrayList;
import java.util.List;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

  private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
  private static final long ANIMATION_DELAY = 80L;
  private static final int CURRENT_POINT_OPACITY = 0xA0;
  private static final int MAX_RESULT_POINTS = 20;
  private static final int POINT_SIZE = 6;
  private final int ScreenRate;

  private CameraManager cameraManager;
  private final Paint paint;
  private Bitmap resultBitmap;
  private int maskColor;
  private int resultColor;
  private int laserColor;
  private int resultPointColor;
  private int scannerAlpha;
  private int borderColor;
  private int cornerColor;
  private int cornerWidth = 10;
  private List<ResultPoint> possibleResultPoints;
  private List<ResultPoint> lastPossibleResultPoints;
  private float[] borderPoints;
  private float backgroundAlpha;
  private int lineWidth = 0;

  // This constructor is used when the class is built from an XML resource.
  public ViewfinderView(Context context, AttributeSet attrs) {
    super(context, attrs);

    // Initialize these once for performance rather than calling them every time in onDraw().
    paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Resources resources = getResources();
    maskColor = resources.getColor(R.color.viewfinder_mask);
    resultColor = resources.getColor(R.color.result_view);
    laserColor = resources.getColor(R.color.viewfinder_laser);
    resultPointColor = resources.getColor(R.color.possible_result_points);
    borderColor = maskColor;
    scannerAlpha = 0;
    possibleResultPoints = new ArrayList<>(5);
    lastPossibleResultPoints = null;
    float density = context.getResources().getDisplayMetrics().density;
    ScreenRate = (int) (20 * density);
    lineWidth = (int) (2 * density);
  }

  public void setCameraManager(CameraManager cameraManager) {
    this.cameraManager = cameraManager;
  }

  public void setMaskColor(int color) {
    maskColor = color;
  }

  public void setBorderColor(int borderColor) {
    this.borderColor = borderColor;
  }

  public void setCornerColor(int cornerColor) {
    this.cornerColor = cornerColor;
  }

  public void setCornerWidth(int cornerWidth) {
    float density = getContext().getResources().getDisplayMetrics().density;
    this.cornerWidth = (int) (cornerWidth * density);
  }

  public void setBackgroundAlpha(float backgroundAlpha) {
    this.backgroundAlpha = backgroundAlpha;
    maskColor = Color.argb((int) (255 * backgroundAlpha), Color.red(maskColor), Color.green(maskColor), Color.blue(maskColor));
  }

  @SuppressLint("DrawAllocation")
  @Override
  public void onDraw(Canvas canvas) {
    if (cameraManager == null) {
      return; // not ready yet, early draw before done configuring
    }
    Rect frame = cameraManager.getFramingRect();
    Rect previewFrame = cameraManager.getFramingRectInPreview();    
    if (frame == null || previewFrame == null) {
      return;
    }
    int width = canvas.getWidth();
    int height = canvas.getHeight();

    // Draw the exterior (i.e. outside the framing rect) darkened
    paint.setColor(resultBitmap != null ? resultColor : maskColor);
    canvas.drawRect(0, 0, width, frame.top, paint);
    canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
    canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
    canvas.drawRect(0, frame.bottom + 1, width, height, paint);

    //draw border
    paint.setColor(borderColor);
    if (lineWidth > 0) {
      paint.setStrokeWidth(lineWidth);
    }
    borderPoints = new float[]{frame.left, frame.top, frame.left, frame.bottom + 1,
                    frame.left, frame.bottom + 1, frame.right, frame.bottom + 1,
                    frame.right, frame.bottom + 1, frame.right, frame.top,
                    frame.right, frame.top, frame.left, frame.top};
    canvas.drawLines(borderPoints, paint);

    if (resultBitmap != null) {
      // Draw the opaque result bitmap over the scanning rectangle
      paint.setAlpha(CURRENT_POINT_OPACITY);
      canvas.drawBitmap(resultBitmap, null, frame, paint);
    } else {
      //draw rect cornor
      paint.setColor(cornerColor);
      canvas.drawRect(frame.left, frame.top, frame.left + ScreenRate,
              frame.top + cornerWidth, paint);
      canvas.drawRect(frame.left, frame.top, frame.left + cornerWidth,
              frame.top + ScreenRate, paint);
      canvas.drawRect(frame.right - ScreenRate, frame.top, frame.right,
              frame.top + cornerWidth, paint);
      canvas.drawRect(frame.right - cornerWidth, frame.top, frame.right,
              frame.top + ScreenRate, paint);
      canvas.drawRect(frame.left, frame.bottom - cornerWidth, frame.left
              + ScreenRate, frame.bottom, paint);
      canvas.drawRect(frame.left, frame.bottom - ScreenRate, frame.left
              + cornerWidth, frame.bottom, paint);
      canvas.drawRect(frame.right - ScreenRate, frame.bottom
              - cornerWidth, frame.right, frame.bottom, paint);
      canvas.drawRect(frame.right - cornerWidth, frame.bottom
              - ScreenRate, frame.right, frame.bottom, paint);

      // Draw a red "laser scanner" line through the middle to show decoding is active
      /*paint.setColor(laserColor);
      paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
      scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
      int middle = frame.height() / 2 + frame.top;
      canvas.drawRect(frame.left + 2, middle - 1, frame.right - 1, middle + 2, paint);*/
      
      float scaleX = frame.width() / (float) previewFrame.width();
      float scaleY = frame.height() / (float) previewFrame.height();

      List<ResultPoint> currentPossible = possibleResultPoints;
      List<ResultPoint> currentLast = lastPossibleResultPoints;
      int frameLeft = frame.left;
      int frameTop = frame.top;
      if (currentPossible.isEmpty()) {
        lastPossibleResultPoints = null;
      } else {
        possibleResultPoints = new ArrayList<>(5);
        lastPossibleResultPoints = currentPossible;
        paint.setAlpha(CURRENT_POINT_OPACITY);
        paint.setColor(resultPointColor);
        synchronized (currentPossible) {
          for (ResultPoint point : currentPossible) {
            canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                              frameTop + (int) (point.getY() * scaleY),
                              POINT_SIZE, paint);
          }
        }
      }
      if (currentLast != null) {
        paint.setAlpha(CURRENT_POINT_OPACITY / 2);
        paint.setColor(resultPointColor);
        synchronized (currentLast) {
          float radius = POINT_SIZE / 2.0f;
          for (ResultPoint point : currentLast) {
            canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                              frameTop + (int) (point.getY() * scaleY),
                              radius, paint);
          }
        }
      }

      // Request another update at the animation interval, but only repaint the laser line,
      // not the entire viewfinder mask.
      postInvalidateDelayed(ANIMATION_DELAY,
                            frame.left - POINT_SIZE,
                            frame.top - POINT_SIZE,
                            frame.right + POINT_SIZE,
                            frame.bottom + POINT_SIZE);
    }
  }

  public void drawViewfinder() {
    Bitmap resultBitmap = this.resultBitmap;
    this.resultBitmap = null;
    if (resultBitmap != null) {
      resultBitmap.recycle();
    }
    invalidate();
  }

  /**
   * Draw a bitmap with the result points highlighted instead of the live scanning display.
   *
   * @param barcode An image of the decoded barcode.
   */
  public void drawResultBitmap(Bitmap barcode) {
    resultBitmap = barcode;
    invalidate();
  }

  public void addPossibleResultPoint(ResultPoint point) {
    List<ResultPoint> points = possibleResultPoints;
    synchronized (points) {
      points.add(point);
      int size = points.size();
      if (size > MAX_RESULT_POINTS) {
        // trim it
        points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
      }
    }
  }

}
