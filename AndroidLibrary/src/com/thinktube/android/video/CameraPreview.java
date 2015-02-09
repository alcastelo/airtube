package com.thinktube.android.video;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
	private static final String TAG = "CameraPreview";
	SurfaceHolder mHolder;
	Camera mCamera;
	private Camera.PreviewCallback previewCallback;
	private CameraPreviewEventListenerInterface cameraReadyListener;
	private int width, height;
	private boolean portrait;
	private byte[] previewBuffer1, previewBuffer2;
	private boolean surfaceReady;

	// necessary for automatic instantiation from view.xml
	public CameraPreview(Context context, AttributeSet attrs) {
		super(context, attrs);
		if (isInEditMode()) return;
		// this should make sure it is shown above of another SurfaceView
		setZOrderMediaOverlay(true);
	}

	/**
	 * Needs to be called after instantiation
	 * This is done to facilitate future modifications to the SurfaceView handling
	 */
	public void init(Activity act, int width, int height) {
		Log.d(TAG, "init with " + width + "x" + height + " (buffer size: " + PixelFormat.getYV12PreviewSize(width, height) + ")");
		mHolder = getHolder();
		mHolder.addCallback(this);
		this.width = width;
		this.height = height;
		this.portrait = (height > width);
		previewBuffer1 = new byte[PixelFormat.getYV12PreviewSize(width, height)];
		previewBuffer2 = new byte[PixelFormat.getYV12PreviewSize(width, height)];

		if (mCamera != null) {
			// camera.release() is the only way I know to clear the callback buffers... :(
			closeCamera();
		}

		mCamera = Camera.open(findFrontFacingCamera());

		mCamera.addCallbackBuffer(previewBuffer1);
		mCamera.addCallbackBuffer(previewBuffer2);
		mCamera.setPreviewCallbackWithBuffer(previewCallback);

		setCameraDisplayOrientation(act, findFrontFacingCamera(), mCamera);
		setCameraParameters();

		if (surfaceReady) {
			//Log.d(TAG, "mHolder " + mHolder + " sHolder"  + sHolder);
			// in case the surface was created before but we need to re-init
			try {
				mCamera.setPreviewDisplay(mHolder);
				mCamera.startPreview();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void surfaceCreated(SurfaceHolder holder) {
		// The Surface has been created, acquire the camera and tell it where
		// to draw.
		Log.d(TAG, "SURFACE created");
		Log.d(TAG, "Thread ID = " + Thread.currentThread().getId());

		if (mCamera != null) {
			try {
				mCamera.setPreviewDisplay(holder);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// Surface will be destroyed when we return, so stop the camera.
		// important to release it when the activity is stopped.
		Log.d(TAG, "SURFACE destroyed");
		if (cameraReadyListener != null) {
			cameraReadyListener.onPreviewGone();
		}
		closeCamera();
		surfaceReady = false;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// Now that the size is known, set up the camera parameters and begin
		// the preview.
		Log.d(TAG, "SURFACE changed: " + width + "x" + height + (portrait ? " portrait" : " landscape"));
		Log.d(TAG, "SURFACE changed: real size " + w + "x" + h);

		surfaceReady = true;

		if (mCamera != null) {
			mCamera.startPreview();
		}

		if (cameraReadyListener != null) {
			cameraReadyListener.onPreviewReady();
		}
	}

	private void setCameraParameters() {
		Camera.Parameters parameters = mCamera.getParameters();

		/*
		 * On Nexus S YV12 cannot be used, see:
		 * http://code.google.com/p/android/issues/detail?id=36868
		 * http://code.google.com/p/android/issues/detail?id=37655
		 */
		if (android.os.Build.MODEL.compareTo("Nexus S") == 0)
			parameters.setPreviewFormat(ImageFormat.NV21);
		else
			parameters.setPreviewFormat(ImageFormat.YV12);

		//parameters.setPreviewFpsRange(4000,60000);

		// the camera preview size is always like in landscape mode (eg. 640x480, not 480x640)
		if (portrait)
			parameters.setPreviewSize(height, width);
		else
			parameters.setPreviewSize(width, height);

		// the following does not make a difference:
		//parameters.setRotation(90);
		//parameters.set("orientation", "portrait");

		// log the fps values available and set the lowest:
		int count=1;
		for (int[] x : parameters.getSupportedPreviewFpsRange()) {
			Log.d(TAG, "fpsRange " + count + ": " + x[Parameters.PREVIEW_FPS_MIN_INDEX] + " ~ " + x[Parameters.PREVIEW_FPS_MAX_INDEX]);
			if (count++ == 1) {
				Log.d(TAG, "setting this range");
				parameters.setPreviewFpsRange(x[Parameters.PREVIEW_FPS_MIN_INDEX], x[Parameters.PREVIEW_FPS_MAX_INDEX]);
			}
		}

		parameters.setRecordingHint(true);

		mCamera.setParameters(parameters);

		/*
		for (Integer x : parameters.getSupportedPreviewFormats()) {
			Log.d(TAG, "previewformat " + x);
		}
		*/

		// confirm camera parameters
		parameters = mCamera.getParameters();
		int[] range = new int[2];
		parameters.getPreviewFpsRange(range);
		Log.d(TAG, "current fpsRange: " + range[Parameters.PREVIEW_FPS_MIN_INDEX] + " ~ " + range[Parameters.PREVIEW_FPS_MAX_INDEX]);
	}

	public void closeCamera() {
		if (mCamera != null) {
			Log.d(TAG, "closing camera");
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
	}

	private int findFrontFacingCamera() {
		// Search for the front facing camera
		int numberOfCameras = Camera.getNumberOfCameras();
		CameraInfo info = new CameraInfo();

		for (int i = 0; i < numberOfCameras; i++) {
			Camera.getCameraInfo(i, info);
			if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
				return i;
			}
		}
		return 0;
	}

	public void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
		int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
		int degrees = 0;
		int result;

		CameraInfo info = new CameraInfo();
		Camera.getCameraInfo(cameraId, info);

		switch (rotation) {
			case Surface.ROTATION_0: degrees = 0; break;
			case Surface.ROTATION_90: degrees = 90; break;
			case Surface.ROTATION_180: degrees = 180; break;
			case Surface.ROTATION_270: degrees = 270; break;
		}

		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360;  // compensate the mirror
		} else {  // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}

		Log.d(TAG, "set camera orientation " + result);
		camera.setDisplayOrientation(result);
	}

	public void correctAspectRatio(int screenWidth, int screenHeight) {
		android.util.Log.d(TAG, "SPACE: " + screenWidth + "x" + screenHeight);

		LayoutParams lp = getLayoutParams();
		lp.width = screenWidth;
		lp.height = (int) (((float) height / (float) width) * (float) screenWidth);

		if (lp.height > screenHeight) {
			lp.height = screenHeight;
			lp.width = (int) (((float) width / (float) height) * (float) lp.height);
		}

		android.util.Log.d(TAG, "SIZE: " + lp.width + "x" + lp.height + " = " + ((float) lp.width) / lp.height);
		setLayoutParams(lp);
	}

	public void setPreviewCallback(Camera.PreviewCallback cb) {
		previewCallback = cb;
	}

	public Camera getCameraObject() {
		return mCamera;
	}

	/**
	 * Callback Interface for when camera is ready
	 */
	public interface CameraPreviewEventListenerInterface {
		void onPreviewReady();
		void onPreviewGone();

	}

	/**
	 * @param crl the object waiting for the camera
	 */
	public void setCameraReadyListener(CameraPreviewEventListenerInterface crl) {
		cameraReadyListener = crl;
	}

}
