package com.thinktube.android.vision.face;

import android.hardware.Camera;
import android.hardware.Camera.FaceDetectionListener;

/**
 * Class to handle face detection via HW
 *
 * @author Rafael Sierra
 */
public class HwFaceChecker implements FaceCheckerInterface {

	private Camera camera;
	private boolean started = false;

	public static boolean isSupported(Camera camera) {
		Camera.Parameters parameters = camera.getParameters();
		if (parameters.getMaxNumDetectedFaces() > 0) {
			return true;
		}
		return false;
	}

	@Override
	public void init(Camera camera, FaceDetectionListener listener) {
		this.camera = camera;
		camera.setFaceDetectionListener(listener);
	}

	@Override
	public void startDetection() {
		camera.startFaceDetection();
		started = true;
	}

	@Override
	public void stopDetection() {
		camera.stopFaceDetection();
		started = false;
	}

	@Override
	public boolean isStarted() {
		return started;
	}

	@Override
	public float getExpectedFPS() {
		// the camera rate is normally 30fps, so we return that here as this works in real time
		return 30f;
	}
}
