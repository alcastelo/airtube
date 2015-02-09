package com.thinktube.android.vision.face;

import com.thinktube.android.video.CameraPreviewHandler;

import android.hardware.Camera;
import android.hardware.Camera.FaceDetectionListener;
import android.util.Log;

public class FaceChecker implements FaceCheckerInterface {

	private FaceCheckerInterface faceChecker;
	private FaceDetectionListener listener;

	public FaceChecker(Camera camera, CameraPreviewHandler cph) {
		if (HwFaceChecker.isSupported(camera)) {
			faceChecker = new HwFaceChecker();
			Log.i("FaceChecker", "Using HW face detector");
		} else {
			faceChecker = new SwThreadedFaceChecker(new SwFaceDetectorGray(), cph);
			Log.i("FaceChecker", "Using SW face detector");
		}
	}

	@Override
	public boolean isStarted() {
		return faceChecker.isStarted();
	}

	@Override
	public void init(Camera camera, FaceDetectionListener listener) {
		this.listener = listener;
		faceChecker.init(camera, listener);
	}

	@Override
	public void startDetection() {
		faceChecker.startDetection();
	}

	@Override
	public void stopDetection() {
		faceChecker.stopDetection();
		if (listener.getClass() == FilteringFaceDetectionListener.class) {
			((FilteringFaceDetectionListener)listener).reset();
		}
	}

	@Override
	public float getExpectedFPS() {
		return faceChecker.getExpectedFPS();
	}
}
