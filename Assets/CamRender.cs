using System.Collections;
using UnityEngine;
using UnityEngine.UI;

public class CamRender : MonoBehaviour
{
    public Slider zoomSlider, isoSlider, aeSlider, focusSlider;
    private WebCamTexture webCamTexture;
    private WebCamDevice[] devices;
    private int devID;
    private AndroidJavaObject wrapper;
    public RawImage image;
    private AndroidJavaClass cameraClass;

    IEnumerator Start()
    {
        var tf = image.GetComponent<RectTransform>();
        tf.sizeDelta = new Vector2(Screen.width, 1.33f * Screen.width);
        cameraClass = new AndroidJavaClass("com.yun.webCam.Camera2Test");
        yield return Application.RequestUserAuthorization(UserAuthorization.WebCam);
        if (Application.HasUserAuthorization(UserAuthorization.WebCam))
        {
            devices = WebCamTexture.devices;
            if (devices?.Length > 0)
            {
                devID = 0;
                Play(devices[0].name);
#if UNITY_ANDROID && !UNITY_EDITOR
                NativeInit();
#endif
            }
        }

        zoomSlider.onValueChanged.AddListener(SetZoom);
        isoSlider.onValueChanged.AddListener(SetISO);
        aeSlider.onValueChanged.AddListener(SetAE);
        focusSlider.onValueChanged.AddListener(SetFocus);
    }

    private void OnGUI()
    {
        int len = devices?.Length ?? 0;
        for (int i = 0; i < len; i++)
        {
            if (devices != null)
            {
                var t = devices[i].name;
                if (GUI.Button(new Rect(100, 100 * i, 120, 80), t))
                {
                    devID = i;
                    Play(t);
                }
            }
        }
    }

    private void NativeInit()
    {
        var pl_class = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
        var currentActivity = pl_class.GetStatic<AndroidJavaObject>("currentActivity");
        var player = currentActivity.Get<AndroidJavaObject>("mUnityPlayer");
        wrapper = player.Get<AndroidJavaObject>("p"); // Camera2Wrapper
        var b = wrapper.Get<AndroidJavaObject>("b");
        var builder = b.Get<AndroidJavaObject>("t"); // CaptureRequest.Builder
        var camMgr = b.GetStatic<AndroidJavaObject>("b"); // CameraManager
        if (camMgr != null && builder != null)
            cameraClass.CallStatic("SetMgrAndCharacteristics", camMgr, builder, devID.ToString());
    }

    private void SetZoom(float v)
    {
        // webCamTexture.Pause();
        // cameraClass.CallStatic("SetZoom", 8.0f * v);
        // webCamTexture.Play();
        SetCameraCharacteristics("SetZoom", v);
    }

    private void SetISO(float v)
    {
        SetCameraCharacteristics("SetISO", v);
    }

    private void SetAE(float v)
    {
        SetCameraCharacteristics("SetAe", v);
    }

    private void SetFocus(float v)
    {
        SetCameraCharacteristics("SetFocusDistance", v);
    }

    private void SetCameraCharacteristics(string key, float v)
    {
        webCamTexture.Pause();
        cameraClass.CallStatic(key, (int) (100 * v));
        webCamTexture.Play();
    }

    private void Play(string devicename)
    {
        webCamTexture = new WebCamTexture(devicename, 480, 640, 30)
        {
            wrapMode = TextureWrapMode.Repeat
        };
        image.texture = webCamTexture;
        webCamTexture.Play();
    }
}