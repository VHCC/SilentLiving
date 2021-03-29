package com.seetatech.silentliving.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

import com.seetatech.silentliving.constants.ErrorCode;
import com.seetatech.silentliving.mvp.exceptions.CameraUnavailableException;
import com.seetatech.silentliving.utils.CachedStatusAndImage;

import java.util.List;

public class CameraPreview2 extends TextureView implements TextureView.SurfaceTextureListener {

    private static  int CAMERA_ID_FRONT = 1;
    private static  int CAMERA_ID_BACK = 0;

    private static final String TAG = "CameraPreview";
    @Nullable
    private Camera mCamera;
    @Nullable
    private Camera.CameraInfo mCameraInfo;
    private CameraCallbacks mCallbacks;
    private int mRotation;

    public CameraPreview2(Context context) {
        this(context, null);
    }

    public CameraPreview2(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraPreview2(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CameraPreview2(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setSurfaceTextureListener(this);
    }

    public void setCameraCallbacks(CameraCallbacks callbacks) {
        mCallbacks = callbacks;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        try {
            //切换摄像头
            if(CachedStatusAndImage.frontCamera)
            {
                CAMERA_ID_FRONT = 1;//前向
            }
            else
            {
                CAMERA_ID_FRONT = 0;//后向
            }
            openCamera();
            Camera.CameraInfo info = new Camera.CameraInfo();
//            Camera.getCameraInfo(CAMERA_ID_FRONT, info);
            Camera.getCameraInfo(CAMERA_ID_BACK, info);
//            int rotation = Surface.ROTATION_0;
//            if (getContext() instanceof Activity) {
//                rotation = ((Activity) getContext())
//                        .getWindowManager().getDefaultDisplay().getRotation();
//            }
//
//            if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
//                rotation = info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT ? 360 - info.orientation : info.orientation;
//                Log.d(TAG, "orientation: portrait rotation="+rotation);
//            } else {
//                rotation = 90;
//                Log.d(TAG, "orientation: landscape");
//            }
            setCamera(mCamera, info, 90);
//            setCamera(mCamera, info, 0);
            startPreview(surface);
        } catch (Exception e) {
            e.printStackTrace();
            if (mCallbacks != null) {
                mCallbacks.onCameraUnavailable(ErrorCode.CAMERA_UNAVAILABLE_ERROR);
            }
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        stopPreviewAndFreeCamera();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private void startPreview(SurfaceTexture surface) {
        // The Surface has been created, now tell the camera where to draw the preview.
        if (mCamera == null || mCameraInfo == null) {
            return;
        }
        try {
            mCamera.setPreviewTexture(surface);
            List<Camera.Size> sizes = mCamera.getParameters().getSupportedPreviewSizes();
            Log.d(TAG, "sizes:> " + sizes.size());

            Camera.Size expected = sizes.get(sizes.size() - 1);
            for (Camera.Size size : sizes) {
                Log.d(TAG, "size.width:> " + size.width + ", size.height:> " + size.height);
                if (size.width == 640 && size.height == 480) {
                    expected = size;
                    break;
                }
            }
            Log.i(TAG, "Preview size is w:" + expected.width + " h:" + expected.height);
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(expected.width, expected.height);
            //parameters.setPreviewFpsRange();
            mCamera.setParameters(parameters);
            // Start camera preview when id scanned. Del by linhx 20170428 begin
            mCamera.startPreview();
            Log.i(TAG, "Camera preview started.");
            if (mCallbacks != null) {
                mCamera.setPreviewCallback(mCallbacks);
            }
            // Start camera preview when id scanned. Del by linhx 20170428 end
        } catch (Exception e) {
            Log.i(TAG, "Error setting camera preview: " + e.getMessage());
            if (mCallbacks != null) {
                mCallbacks.onCameraUnavailable(ErrorCode.CAMERA_UNAVAILABLE_PREVIEW);
            }
        }
    }

    public void pausePreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
        }
    }

    public void restartPreview() {
        if (mCamera != null) {
            mCamera.startPreview();
            if (mCallbacks != null) {
                mCamera.setPreviewCallback(mCallbacks);
            }
        }
    }

    private void openCamera() throws CameraUnavailableException {
        if (Camera.getNumberOfCameras() > 0) {
            try {
//                mCamera = Camera.open(CAMERA_ID_FRONT);
                mCamera = Camera.open(CAMERA_ID_BACK);
                assert mCamera != null;
            } catch (Exception e) {
                throw new CameraUnavailableException(e);
            }
        } else {
            throw new CameraUnavailableException();
        }
    }

    private void stopPreviewAndFreeCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    private void setCamera(Camera camera, Camera.CameraInfo cameraInfo,
                           int displayOrientation) {
        mCamera = camera;
        mCameraInfo = cameraInfo;
        mCamera.setDisplayOrientation(displayOrientation);
        mRotation = displayOrientation;
    }

    public int getCameraRotation() {
        return mRotation;
    }
}
