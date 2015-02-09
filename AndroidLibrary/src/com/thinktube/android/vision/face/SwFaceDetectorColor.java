package com.thinktube.android.vision.face;

import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera.FaceDetectionListener;
import android.util.Log;

/**
 * Class to check for faces on a camera preview
 * this implements the conversion via the JPEG route.
 * @note	Assumes input in NV21
 * @note	This is slow as there is too much data copying
 *
 * @author Rafael Sierra
 */
public class SwFaceDetectorColor extends SwFaceDetectorListenerAdapter {

	final static String TAG = "CamPreviewFaceCheckerColor";
	/**
	 * the RGB565 bitmap holding the camera preview converted
	 */
	Bitmap bmp;

	/**
	 * intermediate buffer for JPEG conversion
	 */
	byte[] dataCopy;

	/**
	 * stream wrapper for dataCopy above
	 */
	ByteArrayOutputStream outStr;

	/**
	 * conversion object
	 */
	YuvImage yuvimage;

	/**
	 * JPEG<-->RGB conversion options
	 */
	BitmapFactory.Options bitmapLoadingOptions = new BitmapFactory.Options();
	Rect rect;

	public SwFaceDetectorColor() {
		// empty
	}

	public SwFaceDetectorColor(FaceDetectionListener fdl, int w, int h) {
		super(fdl, w, h);
	}

	@Override
	public void init(FaceDetectionListener fdl, int w, int h) {
		fdlistener = fdl;
		bitmapLoadingOptions.inPreferredConfig = Bitmap.Config.RGB_565;
		bitmapLoadingOptions.inMutable = true;
		fd = new SwFaceDetector(w, h);
		outStr = new ByteArrayOutputStream(w * h);
		dataCopy = new byte[(int) (w * h * 1.5)]; // Y= 1, Cb=1/4, Cr=1/4
		yuvimage = new YuvImage(dataCopy, ImageFormat.NV21, w, h, null);
		rect = new Rect(0, 0, w, h);
	}

	@Override
	public int check(byte[] data) {
		System.arraycopy(data, 0, dataCopy, 0, data.length);
		yuvimage.compressToJpeg(rect, 100, outStr);
		bmp = BitmapFactory.decodeByteArray(outStr.toByteArray(), 0,
				outStr.size(), bitmapLoadingOptions);
		outStr.reset();
		if (bmp != null) {
			fd.setBitmap(bmp); // need this as bmp is regenerated on every call
			final int nf = fd.findFaces();
			Log.i(TAG, "::check: # faces found: " + nf);
			return nf;
		} else {
			Log.i(TAG, "::check: bmp is null");
			return 0;
		}
	}

}
