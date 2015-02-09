package com.thinktube.airtube.android;

import android.view.Surface;

import com.thinktube.airtube.AirTubeConnectionCallbackI;
import com.thinktube.airtube.AirTubeInterfaceI;
import com.thinktube.android.video.CameraPreview;

public class AudioVideoDuplex  implements AirTubeConnectionCallbackI {
	private AudioDuplex ad;
	private VideoDuplex vd;

	public AudioVideoDuplex(CameraPreview preview) {
		ad = new AudioDuplex();
		vd = new VideoDuplex(preview);
	}

	@Override
	public void onConnect(AirTubeInterfaceI airtube) {
		ad.onConnect(airtube);
		vd.onConnect(airtube);
	}

	@Override
	public void onDisconnect() {
		ad.onDisconnect();
		vd.onDisconnect();
	}

	public void start() {
		ad.start();
		vd.start();
	}

	public void stop() {
		ad.stop();
		vd.stop();
	}

	public void unregister() {
		ad.unregister();
		vd.unregister();
	}

	public void setSurface(Surface surface) {
		vd.setSurface(surface);
	}
}
