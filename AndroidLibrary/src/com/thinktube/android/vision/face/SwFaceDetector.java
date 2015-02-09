package com.thinktube.android.vision.face;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;

public class SwFaceDetector {

	public static int MAX_FACES = 5;

	Face[] faces = new Face[MAX_FACES];
	FaceDetector faceDetector;

	Bitmap bmp;
	int numFaces = 0;

	public SwFaceDetector(Bitmap _bmp) {
		setBitmap(_bmp);
		faceDetector = new FaceDetector(bmp.getWidth(), bmp.getHeight(),
				MAX_FACES);
	}

	public SwFaceDetector(int w, int h) {
		faceDetector = new FaceDetector(w, h, MAX_FACES);
	}

	/**
	 * Caller must ensure bitmap is set before calling
	 * @return
	 */
	public int findFaces() {
		numFaces = faceDetector.findFaces(bmp, faces);
		return numFaces;
	}

	/**
	 * returns an array of the most recently detected faces Rects
	 * must call only if findFaces() returns a number larger than zero
	 * @return
	 */
	public Rect[] getFaces() {
		Rect[] faceRects = new Rect[numFaces];

		for (int i = 0; i < numFaces; i++) {
			if (faces[i] != null) {
				final float eyeDistance = faces[i].eyesDistance();
				final PointF midPoint = new PointF();
				faces[i].getMidPoint(midPoint);

				faceRects[i] = new Rect((int) (midPoint.x - eyeDistance),
						(int) (midPoint.x - eyeDistance),
						(int) (midPoint.x + eyeDistance),
						(int) (midPoint.x + eyeDistance));
			}
		}
		return faceRects;
	}

	public void setBitmap(Bitmap _bmp) {
		bmp = _bmp;
	}
}
