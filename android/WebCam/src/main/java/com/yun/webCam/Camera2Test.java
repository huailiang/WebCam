package com.yun.webCam;

import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.util.Log;

public class Camera2Test
{
    static String TAG = "Unity";

    private static void Print2(int a, int b)
    {
        Log.d(TAG, "Print2 *********** " + a + " b:" + b);
    }

    private static CameraCharacteristics mCameraCharacteristics;
    private static CaptureRequest.Builder mPreviewRequestBuilder;
    private static CameraManager mCameraManager;

    private static final float DEFAULT_ZOOM_FACTOR = 1.0f;
    private static Rect mCropRegion = new Rect();
    public static float maxZoom;
    private static float mCurrentZoomFactor = DEFAULT_ZOOM_FACTOR;
    private static Rect mSensorSize;
    public static boolean hasSupport;


    public static void SetMgrAndCharacteristics(CameraManager mgr, CaptureRequest.Builder b, String name)
    {
        Log.d(TAG, "SetMgrAndCharacteristics: " + (b == null));
        mCameraManager = mgr;
        mPreviewRequestBuilder = b;
        try {
            mCameraCharacteristics = mCameraManager.getCameraCharacteristics(name);
            Zoom(mCameraCharacteristics);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void Zoom(final CameraCharacteristics characteristics)
    {
        mSensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        if (mSensorSize == null) {
            maxZoom = DEFAULT_ZOOM_FACTOR;
            hasSupport = false;
            Log.e(TAG, "NOT SUPPORT");
            return;
        }

        final Float value = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        maxZoom = ((value == null) || (value < DEFAULT_ZOOM_FACTOR)) ? DEFAULT_ZOOM_FACTOR : value;
        hasSupport = (Float.compare(maxZoom, DEFAULT_ZOOM_FACTOR) > 0);
        Log.d(TAG, "maxZoom: " + maxZoom + " default:" + DEFAULT_ZOOM_FACTOR + "  hasSupport: " + hasSupport);
    }


    public static void SetZoom(float newZoom)
    {
        if (!hasSupport) return;

        if (newZoom < DEFAULT_ZOOM_FACTOR) newZoom = DEFAULT_ZOOM_FACTOR;
        if (newZoom > maxZoom) newZoom = maxZoom;
        mCurrentZoomFactor = newZoom;
        final int centerX = mSensorSize.width() / 2;
        final int centerY = mSensorSize.height() / 2;
        final int deltaX = (int) ((0.5f * mSensorSize.width()) / mCurrentZoomFactor);
        final int deltaY = (int) ((0.5f * mSensorSize.height()) / mCurrentZoomFactor);
        mCropRegion.set(centerX - deltaX,
                centerY - deltaY,
                centerX + deltaX,
                centerY + deltaY);

        mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, mCropRegion);
        Log.d(TAG, "SetZoom: " + mCurrentZoomFactor + "  rect: " + mCropRegion);
    }

}