package com.thinktube.android.vision.face;

import android.hardware.Camera;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;

public class FilteringFaceDetectionListener implements FaceDetectionListener {
	/**
	 * a filter before notifying face on / face off
	 */
	private final FaceFilterInterface faceFilter;

	private FaceDetectionListener listener;

	private boolean faceDetected = false;

	public FilteringFaceDetectionListener(FaceDetectionListener listener, float fps) {
		this.listener = listener;
		faceFilter = new FaceFilterCount(fps);
	}

	@Override
	public void onFaceDetection(Face[] faces, Camera camera) {
		if (faces.length > 0) {
			//Log.d("FC", "faces detected");
			if (!faceDetected && faceFilter.checkIn()) {
				faceDetected = true;
				listener.onFaceDetection(faces, camera);
			}
		} else { // no face case
			//Log.d("FC", "no faces detected");
			if (faceDetected && faceFilter.checkOut()) {
				faceDetected = false;
				listener.onFaceDetection(faces, camera);
			}
		}
	}

	public void reset() {
		faceDetected = false;
	}

	public interface FaceFilterInterface {
		boolean checkIn();
		boolean checkOut();
	}

	public class FaceFilterDummy implements FaceFilterInterface {
		@Override
		public boolean checkIn() {
			return true;
		}

		@Override
		public boolean checkOut() {
			return true;
		}
	}

	public class FaceFilterCount implements FaceFilterInterface {

		int countIn = 0;
		int countOut = 0;
		final int minIn; // min num of hits before declaring face ON
		final int maxOut; // max num of hits before declaring face OFF

		public FaceFilterCount(float inputRate) {
			// we use 1s for in and 2s for out
			minIn = (int) inputRate;
			maxOut = 2 * minIn;
		}

		@Override
		public boolean checkIn() {
			if (++countIn >= minIn) {
				resetCount();
				return true;
			}
			return false;
		}

		@Override
		public boolean checkOut() {
			if (++countOut >= maxOut) {
				resetCount();
				return true;
			}
			return false;
		}

		private void resetCount() {
			countIn = 0;
			countOut = 0;
		}
	}
}
