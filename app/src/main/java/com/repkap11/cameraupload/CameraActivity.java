package com.repkap11.cameraupload;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback, View.OnClickListener {

    private static final String TAG = CameraActivity.class.getSimpleName();
    private SurfaceView mCameraSurfaceView;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Button mTakePictureButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        mCameraSurfaceView = (SurfaceView) findViewById(R.id.camera_stream);
        mTakePictureButton = (Button) findViewById(R.id.take_picture);
        mTakePictureButton.setOnClickListener(this);


    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        openCamera(1);
        if (mCamera == null) {
            Log.v(TAG, "Camera not available");
        }
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mCameraSurfaceView.getHolder();
        mHolder.addCallback(this);
    }

    private void openCamera(final int retryCount) {
        int numberOfCameras = Camera.getNumberOfCameras();
        Log.v(TAG, "numberOfCameras:" + numberOfCameras);
        int cameraID = 0;
        mCamera = Camera.open(cameraID);
        Log.v(TAG, "Standard camera open worked");
        mCamera.setErrorCallback(new Camera.ErrorCallback() {
            @Override
            public void onError(int error, Camera camera) {
                Log.v(TAG, "Camera Error:" + error + " on attempt:" + retryCount);
            }

        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startCameraPreview(holder);

    }

    private void startCameraPreview(SurfaceHolder holder) {// The Surface has been created, now tell the camera where to draw the preview.
        if (mCamera != null) {
            Log.v(TAG, "Starting to create camera");
            try {
                mCamera.setPreviewDisplay(holder);
                Log.v(TAG, "Preview Display Set");
                mCamera.startPreview();
                Log.v(TAG, "Preview Started");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.v(TAG, "Surface Changes: Size W:" + w + " H:" + h);
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
                Camera.Parameters params = mCamera.getParameters();
                //printCameraInfo(params);
                //TODO select a size that matches the aspect ratio of the surface, not just the first one (camera1 doesn't matter)
                //These must be from the list params.getSupportedPreviewSizes()
                //params.setPreviewSize(2048, 1536);

                Camera.Size firstSize = params.getSupportedPreviewSizes().get(0);
                params.setPreviewSize(firstSize.width, firstSize.height);
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

                mCamera.setParameters(params);
                mCamera.setPreviewDisplay(mHolder);
                setCameraDisplayOrientation(this, 0, mCamera);
                mCamera.startPreview();
            } catch (Exception e) {
                Log.v(TAG, "Error starting Camera preview: " + e.getMessage());
            }
        } else {
            Log.v(TAG, "surfaceChanged, but camera null");
        }
    }

    public static void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
        Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v(TAG, "Surface Destroyed");
        killCamera();
    }

    private void killCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            Log.v(TAG, "Camera Released");
            mCamera = null;
        }
    }

    @Override
    protected void onPause() {
        killCamera();
        super.onPause();
    }

    private void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE //dont resize when system bars visible
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION// dont resize when system bars visible
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onClick(View v) {
        if (mCamera != null) {
            mCamera.takePicture(new Camera.ShutterCallback() {
                @Override
                public void onShutter() {

                }
            }, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    uploadBytes(data);
                }
            });
        }
    }

    private void uploadBytes(byte[] data) {
        try {
            URLConnection c = new URL(url).openConnection();
            String username = "guest";
            String password = "guest";
            String userPassword = username + ":" + password;
            String encoding = Base64.encodeToString(userPassword.getBytes(), Base64.DEFAULT);
            c.setRequestProperty("Authorization", "Basic " + encoding);
            c.setUseCaches(false);
            c.getOutputStream().write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
