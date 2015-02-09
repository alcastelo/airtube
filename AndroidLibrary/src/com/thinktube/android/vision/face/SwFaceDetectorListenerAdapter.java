package com.thinktube.android.vision.face;

import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;

public abstract class SwFaceDetectorListenerAdapter {

	/**
	 * the actual face detector
	 */
	protected SwFaceDetector fd;

	/**
	 * called when a face is detected
	 */
	protected FaceDetectionListener fdlistener;

	/**
	 * dummy for when no faces are detected
	 */
	private Face[] zeroFaces = new Face[0]; ;

	/**
	 * dummy returned via callback when faces are detected:
	 */
	private Face[] oneFace = new Face[1]; ;

	/**
	 * Constructs a face checker
	 * @param w	width of the input
	 * @param h height of the input
	 */
	public SwFaceDetectorListenerAdapter(FaceDetectionListener fdl, int w, int h) {
		init(fdl, w, h);
	}

	public SwFaceDetectorListenerAdapter() {
		// empty
	}

	abstract public void init(FaceDetectionListener fdl, int w, int h);

	public void onPreviewFrame(byte[] data) {
		if ( check(data) > 0 ) {
			// TODO: pass the actual array of faces objects properly converted to "android.hardware.Camera.Face"
			fdlistener.onFaceDetection(oneFace, null);
		} else  {
			// just a hack to signal there were no faces found. just to avoid recreating instances of the list
			fdlistener.onFaceDetection(zeroFaces, null);
		}
	}

	/**
	 * The actual check implemented by concrete subclasses.
	 * @param data
	 * @return
	 */
	abstract public int check(byte[] data);
}
