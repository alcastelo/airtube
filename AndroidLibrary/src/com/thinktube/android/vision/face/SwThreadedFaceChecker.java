package com.thinktube.android.vision.face;

import com.thinktube.android.video.CameraPreviewHandler;

import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.Size;
import android.hardware.Camera;

/**
 * Decorates a CamPreviewFaceCheckerBase adding thread processing to the onPreviewFrame callback
 * @author Rafael Sierra
 *
 */
public class SwThreadedFaceChecker implements FaceCheckerInterface, Camera.PreviewCallback {

	SwFaceDetectorListenerAdapter faceChecker;
	private boolean started = false;
	final CameraPreviewHandler camPreviewHandler;

	/**
	 * offload thread for SW face detection
	 */
	private FaceCheckerThread faceCheckerThread;

	public SwThreadedFaceChecker(SwFaceDetectorListenerAdapter fc, CameraPreviewHandler cph) {
		faceChecker = fc;
		camPreviewHandler = cph;
	}

	@Override
	public void init(Camera camera, FaceDetectionListener listener) {
		Size s = camera.getParameters().getPreviewSize();
		//TODO: swapped with&height for portrait mode
		faceChecker.init(listener, s.height, s.width);
	}

	@Override
	public void onPreviewFrame(final byte[] data, Camera camera) {
		if (started && faceCheckerThread.isIdle()) {
//			If thread is idle copy data pointer and restart process
			faceCheckerThread.setInput(data);
			synchronized (faceCheckerThread) {
				faceCheckerThread.notify();
			}
		}
	}

	@Override
	public boolean isStarted() {
		return started;
	}

	@Override
	public void startDetection() {
		if (faceCheckerThread == null) {
			faceCheckerThread = new FaceCheckerThread();
			faceCheckerThread.setDaemon(true);
			faceCheckerThread.setName("FaceCheckerThread");
			faceCheckerThread.start();
		}
		started = true;
		camPreviewHandler.addListener(this);
	}

	@Override
	public void stopDetection() {
		// for now we don't kill the thread
		started = false;
		camPreviewHandler.removeListener(this);
	}

	@Override
	public float getExpectedFPS() {
		// XXX: SwFaceDetector gives about 5 frames every 2 seconds on N7 & GN, so using that for now
		return 2.5f;
	};

	private class FaceCheckerThread extends Thread {
		volatile byte[] data;
		volatile boolean idle = true;

		@Override
		public void run() {
			for (;;) {
				if (data != null) {
					idle = false;
					faceChecker.onPreviewFrame(data);
				}
				idle = true;
				try {
					synchronized (this) {
						wait();
					}
				} catch (InterruptedException e) {
					break;
				}
			}
			faceCheckerThread = null; // kills itself
		}

		public boolean isIdle() {
			return idle;
		}

		public void setInput(final byte[] data) {
			this.data = data;
		}
	};
}
