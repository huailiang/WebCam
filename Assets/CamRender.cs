using System.Collections;
using UnityEngine;
using UnityEngine.UI;

public class CamRender : MonoBehaviour
{
    private float slider;
    private WebCamTexture webCamTexture;
    private WebCamDevice[] devices;
    private int devID;
    private AndroidJavaObject wrapper;
    public RawImage image;

    IEnumerator Start()
    {
        yield return Application.RequestUserAuthorization(UserAuthorization.WebCam);
        if (Application.HasUserAuthorization(UserAuthorization.WebCam))
        {
            devices = WebCamTexture.devices;
            if (devices?.Length > 0)
            {
                devID = 0;
                Play(devices[0].name);
            }
        }
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
        if (GUI.Button(new Rect(240, 0, 120, 80), "ZoomInit"))
        {
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
        }
        if (GUI.Button(new Rect(240, 100, 120, 80), "ZoomSet"))
        {
            webCamTexture.Pause();
            var pl_class = new AndroidJavaClass("com.yun.webCam.Camera2Test");
            pl_class.CallStatic("SetZoom", slider);
            webCamTexture.Play();
        } 
        slider = GUI.HorizontalSlider(new Rect(240, 320, 520, 80), slider, 0.1f, 8.0f);
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