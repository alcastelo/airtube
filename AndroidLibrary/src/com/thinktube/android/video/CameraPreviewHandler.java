/**
 *
 */
package com.thinktube.android.video;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.hardware.Camera;
import android.util.Log;

/**
 * @author Rafael Sierra
 *
 */
public class CameraPreviewHandler implements Camera.PreviewCallback {

	public List<Camera.PreviewCallback> listeners = new CopyOnWriteArrayList<Camera.PreviewCallback>();
	private byte[] rotatedData;
	private boolean portrait;
	private int width, height;

	public CameraPreviewHandler(int width, int height) {
		this.width = width;
		this.height = height;
		this.portrait = (height > width);
		if (portrait)
			rotatedData = new byte[PixelFormat.getYV12PreviewSize(width, height)];
		Log.d("CameraPreviewHandler", "new " + this + " ("+ width + "x" + height + ")");
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		// this should maybe be made more flexible, like a list of data-modifying callbacks?
		if (portrait)
			PixelFormat.rotateYV12_270(data, rotatedData, height, width); // we need to put the w and h of the original data
		else
			rotatedData = data;

		for (Camera.PreviewCallback listener : listeners) {
			listener.onPreviewFrame(rotatedData, camera);
		}

		camera.addCallbackBuffer(data);
	}

	public synchronized void addListener(Camera.PreviewCallback listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
		Log.d("CameraPreviewHandler", "addListener: new number of listeners = " + listeners.size());
	}

	public void removeListener(Camera.PreviewCallback listener) {
		listeners.remove(listener);
		Log.d("CameraPreviewHandler", "removeListener: new number of listeners = " + listeners.size());
	}

}
