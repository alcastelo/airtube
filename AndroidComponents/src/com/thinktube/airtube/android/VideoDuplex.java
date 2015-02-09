package com.thinktube.airtube.android;

import android.view.Surface;

import com.thinktube.airtube.AirTubeConnectionCallbackI;
import com.thinktube.airtube.AirTubeID;
import com.thinktube.airtube.AirTubeInterfaceI;
import com.thinktube.android.video.CameraPreview;
import com.thinktube.android.video.CameraPreviewHandler;

public class VideoDuplex implements AirTubeConnectionCallbackI {
	private VideoService vs;
	private VideoClient vc;

	public VideoDuplex(CameraPreview preview) {
		vs = new VideoService(preview);
		vc = new VideoClient();
	}

	public VideoDuplex(CameraPreview preview, boolean passive) {
		vs = new VideoService(preview);
		vc = new VideoClient(passive);
	}

	public VideoDuplex(CameraPreviewHandler camPreviewH, boolean passive) {
		vs = new VideoService(camPreviewH);
		vc = new VideoClient(passive);
	}

	@Override
	public void onConnect(AirTubeInterfaceI airtube) {
		vs.onConnect(airtube);
		vc.onConnect(airtube);
	}

	@Override
	public void onDisconnect() {
		vs.onDisconnect();
		vc.onDisconnect();
	}

	public void start() {
		vs.start();
		vc.start();
	}

	public void stop() {
		vs.stop();
		vc.stop();
	}

	public void unregister() {
		vs.unregister();
		vc.unregister();
	}

	public void setSurface(Surface surface) {
		vc.setSurface(surface);
	}

	public void subscribeTo(AirTubeID sid) {
		vc.subscribeTo(sid);
	}

	public void unsubscribe() {
		vc.unsubscribe();
	}

	public AirTubeID getServiceId() {
		return vs.getServiceId();
	}
}
