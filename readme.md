

>获取物理相机的预览流是AR开发的基础能力， 在 ARFoundation 和 AREngine 都是使用 CommandBuffer 来绘制预览流。 如果使用WebCameraTexture获取的预览流受限制比较多， 比如说去调整变焦、曝光度这些东西更多的还是去原生层去拿。 




### Unity访问camera2


问题是unity也没有Camera.open()这样的api, 也没有提供  Camera getRunningCamera() 类似这样的api， 获取当前webCameraTexture在session的句柄。 通过解压安装包的 classes.jar 发现， 其实现也是基于 Camera2的api, 比如说 Camera2Wrapper 这个类就是对外访问的wrapper。   在Unity2018中的jar包中， 有个类（混淆过）叫a.class 存在包 com.unity3d.player 中， 可以看到完整的camera2的调用的过程。


这个a的实例就存放在 Camera2Wrapper.class,  而Camera2Wrapper的实例存在 这个类里了， 从上图可以看到很多成员都是私有/private的， 直接调用肯定是没有权限的， 但是c#里提供了AndroidJavaClass 却可以访问到， 这里猜测其内部实现通过反射拿到的， 效率肯定高不到哪里去， 但这种获取句柄的操作 往往是初始化的调用一次， 并不是每帧调用， 所以也不会有额外的功耗开销。

```csharp
var pl_class = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
var currentActivity = pl_class.GetStatic<AndroidJavaObject>("currentActivity");
var player = currentActivity.Get<AndroidJavaObject>("mUnityPlayer");
wrapper = player.Get<AndroidJavaObject>("p"); // Camera2Wrapper
var b = wrapper.Get<AndroidJavaObject>("b");
var builder = b.Get<AndroidJavaObject>("t"); // CaptureRequest.Builder
var camMgr = b.GetStatic<AndroidJavaObject>("b"); // CameraManager
if (camMgr != null && builder != null)
{
    pl_class = new AndroidJavaClass("com.yun.webCam.Camera2Test");
    pl_class.CallStatic("SetMgrAndCharacteristics", camMgr, builder, devID.ToString());
}
```

作者就是这样拿到 CameraManager 和 CaptureRequest.Builder 的， 拿到之后传递到Camera2Test这个自己实现的类中， 然后就这可以做各种效果了。Camera2Test.java 类实现：


```java
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
```


大致思路就是如此了， 需要注意的是 逆向 classes.jar, 每个版本的unity里的变量名都是一致的， 最好还是去安装包去看下实际的变量名， 然后在获取传递到原生层去。 项目的代码我已经上传到 [github][i4]了。


[i1]: https://www.jianshu.com/p/9a2e66916fcb
[i3]: https://forum.unity.com/threads/webcamtexture-on-android-focus-mode-fix.327956/
[i4]: https://github.com/huailiang/WebCam