package com.google.zxing.client.android;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Handler;

import com.google.zxing.Result;
import com.google.zxing.client.android.camera.CameraManager;

/**
 * Created by xunce on 2018/1/11.
 */

public interface CaptureListener {
    ViewfinderView getViewfinderView();
    Handler getHandler();
    CameraManager getCameraManager();
    void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor);
    void setResult(int resultCode, Intent data);
    void finish();
    void drawViewfinder();
    PackageManager getPackageManager();
}
