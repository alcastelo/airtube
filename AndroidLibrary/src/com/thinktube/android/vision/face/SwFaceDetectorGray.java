package com.thinktube.android.vision.face;

import java.nio.ShortBuffer;
import android.graphics.Bitmap;
import android.hardware.Camera.FaceDetectionListener;

/**
 * Class to check for faces on a camera preview
 * this implements the using a quick conversion to RGB565 as grayscale
 * Assumes input in NV21
 *
 * @author Rafael Sierra
 */
public class SwFaceDetectorGray extends SwFaceDetectorListenerAdapter {

	final static String TAG = "CamPreviewFaceCheckerGray";
	/**
	 * the RGB565 bitmap holding the camera preview converted
	 */
	Bitmap bmp;

	/**
	 * target buffer for conversion
	 */
	short[] gray565;
	ShortBuffer buf;
	int frameSize;

	public SwFaceDetectorGray() {
		// empty
	}

	public SwFaceDetectorGray(FaceDetectionListener fdl, int w, int h) {
		super(fdl, w, h);
	}

	@Override
	public void init(FaceDetectionListener fdl, int w, int h) {
		fdlistener = fdl;
		frameSize = w * h;
		gray565 = new short[frameSize];
		buf = ShortBuffer.wrap(gray565);
		bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
		fd = new SwFaceDetector(bmp);
	}

	@Override
	public int check(byte[] data) {
		yuv2gray565(data); // fastest
		bmp.copyPixelsFromBuffer(buf);
		buf.position(0);
		final int nf = fd.findFaces();
		//Log.i(TAG, "::check: # faces found: " + nf);
		return nf;
	}

	/**
	 * This converts data to a gray image in rgb565 pixel format
	 * This just extract
	 * @param data	Any YUV image with Y as first plane (all of them?)
	 * @note modifies: gray565:	grayscale result in RGB565, 2 bytes per pixel
	 * @note: uses: frameSize
	 */
	private void yuv2gray565(byte[] data) {
		for (int i = 0; i < frameSize; i++) {
			final int y = 0xff & data[i]; // XXX: must clear rest of bits otherwise image gets corrupted
			final int g = y >> 2; // down to 6 bits
			final int rb = y >> 3; // down to 5 bits. r & b are the same
			gray565[i] = (short) ((rb << 11) | (g << 5) | rb);
		}
	}
}
