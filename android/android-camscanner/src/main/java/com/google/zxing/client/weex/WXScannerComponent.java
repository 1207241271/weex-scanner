package com.google.zxing.client.weex;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.google.zxing.client.android.CaptureActivityHandler;
import com.google.zxing.client.android.CaptureListener;
import com.google.zxing.client.android.R;
import com.google.zxing.client.android.ViewfinderView;
import com.google.zxing.client.android.camera.CameraManager;
import com.google.zxing.client.result.ResultParser;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.dom.WXDomObject;
import com.taobao.weex.ui.component.WXComponent;
import com.taobao.weex.ui.component.WXVContainer;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by xunce on 2018/1/11.
 */

public class WXScannerComponent extends WXComponent implements SurfaceHolder.Callback,
        CaptureListener, WXSDKInstance.OnInstanceVisibleListener {
    public static final String TAG = WXScannerComponent.class.getSimpleName();
    public static final String BORDER_COLOR = "borderColor";
    public static final String CORNER_COLOR = "cornerColor";
    public static final String CORNER_WIDTH = "cornerWidth";
    public static final String BACKGROUD_ALPHA = "backgroundAlpha";
    public static final String DID_FINISH_SCAN = "DidFinishScan";
    private CameraManager cameraManager;
    private ViewfinderView viewfinderView;
    private Result savedResultToShow;
    private boolean hasSurface;
    private View rootView;
    private CaptureActivityHandler handler;
    private Collection<BarcodeFormat> decodeFormats;
    private Map<DecodeHintType, ?> decodeHints;
    private String characterSet;
    private Result lastResult;
    private int borderColor;
    private int cornerColor;
    private int cornerWidth;
    private float backgroudAlpha;

    public WXScannerComponent(WXSDKInstance instance, WXDomObject dom, WXVContainer parent) {
        super(instance, dom, parent);
    }

    @Override
    protected View initComponentHostView(@NonNull Context context) {
        hasSurface = false;
        rootView = LayoutInflater.from(context).inflate(R.layout.scanner_view_layout, null);
        if (getInstance() != null) {
            getInstance().addOnInstanceVisibleListener(this);
        }
        if (getDomObject().getAttrs().get(BORDER_COLOR) != null) {
            borderColor = Color.parseColor((String) getDomObject().getAttrs().get(BORDER_COLOR));
        }
        if (getDomObject().getAttrs().get(CORNER_COLOR) != null) {
            cornerColor = Color.parseColor((String) getDomObject().getAttrs().get(CORNER_COLOR));
        }
        if (getDomObject().getAttrs().get(CORNER_WIDTH) != null) {
            cornerWidth = Integer.decode((String) getDomObject().getAttrs().get(CORNER_WIDTH));
        }
        if (getDomObject().getAttrs().get(BACKGROUD_ALPHA) != null) {
            backgroudAlpha = Float.parseFloat((String) getDomObject().getAttrs().get(BACKGROUD_ALPHA));
        }
        return rootView;
    }

    @Override
    public void onActivityCreate() {
        super.onActivityCreate();
        Dexter.withActivity((Activity) getContext()).
                withPermission(Manifest.permission.CAMERA).
                withListener(new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse response) {

            }

            @Override
            public void onPermissionDenied(PermissionDeniedResponse response) {

            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

            }
        });
    }

    @Override
    public void onActivityPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        cameraManager.closeDriver();
        //historyManager = null; // Keep for onActivityResult
        if (!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) rootView.findViewById(R.id.preview_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        super.onActivityPause();
    }

    @Override
    public void onAppear() {
        Context context = getContext();
        viewfinderView = rootView.findViewById(R.id.viewfinder_view);
        viewfinderView.setBorderColor(borderColor);
        viewfinderView.setCornerColor(cornerColor);
        viewfinderView.setCornerWidth(cornerWidth);
        viewfinderView.setBackgroundAlpha(backgroudAlpha);
        cameraManager = new CameraManager(context.getApplicationContext());
        viewfinderView.setCameraManager(cameraManager);

        SurfaceView surfaceView = (SurfaceView) rootView.findViewById(R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(this);
        }
    }

    @Override
    public void onDisappear() {

    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (handler == null) {
                handler = new CaptureActivityHandler(this, decodeFormats, decodeHints, characterSet, cameraManager);
            }
            decodeOrStoreSavedBitmap(null, null);
        } catch (IOException ioe) {
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            displayFrameworkBugMessageAndExit();
        }
    }

    private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
        // Bitmap isn't used yet -- will be used soon
        if (handler == null) {
            savedResultToShow = result;
        } else {
            if (result != null) {
                savedResultToShow = result;
            }
            if (savedResultToShow != null) {
                Message message = Message.obtain(handler, R.id.decode_succeeded, savedResultToShow);
                handler.sendMessage(message);
            }
            savedResultToShow = null;
        }
    }

    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
        }
    }

    private void displayFrameworkBugMessageAndExit() {
//        if (getContext() == null) return;
//        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
//        builder.setTitle(getContext().getResources().getString(R.string.app_name));
//        builder.setMessage(getContext().getResources().getString(R.string.msg_camera_framework_bug));
//        builder.setPositiveButton(R.string.button_ok, new FinishListener(((Activity)getContext())));
//        builder.setOnCancelListener(new FinishListener(((Activity) getContext())));
//        builder.show();
        HashMap<String, String> map = new HashMap<>();
        map.put("result", "init failed");
        map.put("status", "failed");
        fireEvent(DID_FINISH_SCAN, map);
    }

    @Override
    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    @Override
    public Handler getHandler() {
        return handler;
    }

    @Override
    public CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
        if (lastResult != null && lastResult.getText().equals(rawResult.getText())) {
            restartPreviewAfterDelay(0);
            return;
        }
        lastResult = rawResult;

        handleDecodeInternally(rawResult, barcode);
    }

    private void handleDecodeInternally(Result rawResult,  Bitmap barcode) {
        CharSequence displayContents = ResultParser.parseResult(rawResult).getDisplayResult().replace("\r", "");
        String result = displayContents.toString();
        HashMap<String, String> map = new HashMap<>();
        map.put("result", result);
        map.put("status", "success");
        fireEvent(DID_FINISH_SCAN, map);
        Log.e(TAG, "scan result " + result);
    }

    @Override
    public void setResult(int resultCode, Intent data) {

    }

    @Override
    public void finish() {

    }

    @Override
    public void drawViewfinder() {

    }

    @Override
    public PackageManager getPackageManager() {
        return null;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onActivityStop() {
        super.onActivityStop();
    }

    @Override
    public void onActivityDestroy() {
        super.onActivityDestroy();
        if (getInstance() != null) {
            getInstance().removeOnInstanceVisibleListener(this);
        }
    }

    @Override
    public boolean onActivityBack() {
        return super.onActivityBack();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
