

>获取物理相机的预览流是AR开发的基础能力， 在 ARFoundation 和 AREngine 都是使用 CommandBuffer 来绘制预览流。 如果使用WebCameraTexture获取的预览流受限制比较多， 比如说去调整变焦(ZoomRatio)、 曝光补偿（[AE][i2]）、相机感光度（[ISO][i5])、快门/曝光时间等。 这些参数更多的还是去原生层去拿， 而Camera2里的CameraCharacteristics 提供了一个便捷的接口去访问这些数据。



效果视频地址：

https://www.bilibili.com/video/BV13R4y137e9?share_source=copy_web





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


[i1]: https://www.jianshu.com/p/9a2e66916fcb
[I2]: https://baike.baidu.com/item/AE模式
[i3]: https://forum.unity.com/threads/webcamtexture-on-android-focus-mode-fix.327956/
[i4]: https://github.com/huailiang/WebCam
[i5]: https://baike.baidu.com/item/iso/5397601#viewPageContent
[i6]: https://blog.csdn.net/qq_293