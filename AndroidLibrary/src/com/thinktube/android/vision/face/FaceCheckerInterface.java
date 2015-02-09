package com.thinktube.android.vision.face;

import android.hardware.Camera;
import android.hardware.Camera.FaceDetectionListener;

public interface FaceCheckerInterface {

	/**
	 * @return whether face detection is running
	 */
	boolean isStarted();

	/**
	 * initialize instead of a Constructor
	 *
	 * @param camera
	 * @param listener
	 */
	void init(Camera camera, FaceDetectionListener listener);

	/**
	 * starts processing input
	 */
	void startDetection();

	/**
	 * stops processing input
	 */
	void stopDetection();

	/**
	 * @return	the expected frame rate for this checker: this is HW dependent so the implementations should probably ask for CPU info
	 * 			in order to return a consistent value across devices.
	 */
	float getExpectedFPS();
}
