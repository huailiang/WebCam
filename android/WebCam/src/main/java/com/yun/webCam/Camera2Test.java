package com.yun.webCam;

import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.util.Log;
import android.util.Range;

import java.util.PrimitiveIterator;

public class Camera2Test
{
    static String TAG = "Unity";
    private static CameraCharacteristics mCameraCharacteristics;
    private static CaptureRequest.Builder mPreviewRequestBuilder;
    private static final float DEFAULT_ZOOM_FACTOR = 1.0f;
    private static final Rect mCropRegion = new Rect();
    public static float maxZoom;
    private static Rect mSensorSize;
    public static boolean hasSupport;


    public static void SetMgrAndCharacteristics(CameraManager mgr, CaptureRequest.Builder b, String name)
    {
        Log.d(TAG, "SetMgrAndCharacteristics: " + (b == null));
        mPreviewRequestBuilder = b;
        try {
            mCameraCharacteristics = mgr.getCameraCharacteristics(name);
            GetZoom(mCameraCharacteristics);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void GetZoom(final CameraCharacteristics characteristics)
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

    /**
     * 获取曝光补偿
     *
     * @param i 0-100 百分比
     */
    private static void SetAe(int i)
    {
        Range<Integer> integerRange = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
        int min = integerRange.getLower();
        int max = integerRange.getUpper();
        int all = max - min;
        int time = 100 / all;
        int ae = ((i / time) - max) > max ? max : Math.max(((i / time) - max), min);
        Log.d(TAG, "AE: " + ae + " min: " + min + " max: " + max);
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, ae);
    }


    /**
     * ISO感光灵敏度
     *
     * @param i 0-100 百分比
     */
    private static void SetISO(int i)
    {
        Range<Integer> range = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
        int max = range.getUpper();
        int min = range.getLower();
        int iso = ((i * (max - min)) / 100 + min);
        mPreviewRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, iso);
    }


    /**
     * 设置焦距
     *
     * @param i 0-100 百分比
     */
    private static void SetFocusDistance(int i)
    {
        float minimumLens = mCameraCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        float num = (((float) i) * minimumLens / 100);
        mPreviewRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, num);
    }

    /**
     * 获取缩放
     */
    public static void SetZoom(int zoom)
    {
        if (!hasSupport) return;
        float mCurrentZoomFactor = (maxZoom - DEFAULT_ZOOM_FACTOR) * (zoom / 100.0f) + DEFAULT_ZOOM_FACTOR;
        final int centerX = mSensorSize.width() / 2;
        final int centerY = mSensorSize.height() / 2;
        final int deltaX = (int) ((0.5f * mSensorSize.width()) / mCurrentZoomFactor);
        final int deltaY = (int) ((0.5f * mSensorSize.height()) / mCurrentZoomFactor);
        mCropRegion.set(centerX - deltaX, centerY - deltaY, centerX + deltaX, centerY + deltaY);
        mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, mCropRegion);
        Log.d(TAG, "SetZoom: " + mCurrentZoomFactor + "  rect: " + mCropRegion);
    }

}