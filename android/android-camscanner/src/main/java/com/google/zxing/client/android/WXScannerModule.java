package com.google.zxing.client.android;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.google.zxing.client.android.camera.CameraManager;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.taobao.weex.annotation.JSMethod;
import com.taobao.weex.bridge.JSCallback;
import com.taobao.weex.common.WXModule;
import com.taobao.weex.ui.component.WXComponent;
import com.taobao.weex.ui.component.WXDiv;

/**
 * Created by yangxu on 2017/12/26.
 */

public class WXScannerModule extends WXModule {
    private JSCallback scCallback;
    private Integer scanRequestCode = 200;
    private String cameraRequest = "请打开相机权限";

    @JSMethod
    public void scanQR(String title, final JSCallback callback) {
        final Activity context = (Activity) mWXSDKInstance.getContext();
        if (context == null) return;
        Dexter.withActivity(context).withPermission(Manifest.permission.CAMERA)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        Intent intent = new Intent(context, CaptureActivity.class);
                        scCallback = callback;
                        context.startActivityForResult(intent, scanRequestCode);
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(context, cameraRequest, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        Toast.makeText(context, cameraRequest, Toast.LENGTH_SHORT).show();
                    }
                }).check();
    }

    @JSMethod
    public void closeFlashlight() {
        if (mWXSDKInstance != null && mWXSDKInstance.getContext() != null) {
            WXComponent rootComponent = mWXSDKInstance.getRootComponent();
            if (rootComponent instanceof WXDiv) {
                int childCount = ((WXDiv) rootComponent).getChildCount();
                int index = 0;
                for (index = 0; index < childCount; index++) {
                    if (((WXDiv) rootComponent).getChild(index) instanceof CaptureListener) {
                        CameraManager cameraManager = ((CaptureListener) ((WXDiv) rootComponent).getChild(index)).getCameraManager();
                        if (cameraManager == null) {
                            cameraManager = getCameraManager();
                        }
                        cameraManager.setTorch(false);
                        break;
                    }
                }
                if (index == childCount) {
                    getCameraManager().setTorch(false);
                }
            } else {
                getCameraManager().setTorch(false);
            }
        }
    }

    @JSMethod
    public void openFlashlight() {
        if (mWXSDKInstance != null && mWXSDKInstance.getContext() != null) {
            WXComponent rootComponent = mWXSDKInstance.getRootComponent();
            if (rootComponent instanceof WXDiv) {
                int childCount = ((WXDiv) rootComponent).getChildCount();
                int index = 0;
                for (index = 0; index < childCount; index++) {
                    if (((WXDiv) rootComponent).getChild(index) instanceof CaptureListener) {
                        CameraManager cameraManager = ((CaptureListener) ((WXDiv) rootComponent).getChild(index)).getCameraManager();
                        if (cameraManager == null) {
                            cameraManager = getCameraManager();
                        }
                        cameraManager.setTorch(true);
                        break;
                    }
                }
                if (index == childCount) {
                    getCameraManager().setTorch(true);
                }
            } else {
                getCameraManager().setTorch(true);
            }
        }
    }

    private CameraManager getCameraManager() {
        return new CameraManager(mWXSDKInstance.getContext());
    }
}
