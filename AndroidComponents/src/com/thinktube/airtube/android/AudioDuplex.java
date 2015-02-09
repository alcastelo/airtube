package com.thinktube.airtube.android;

import com.thinktube.airtube.*;
import com.thinktube.audio.JitterBuffer.Statistics;

public class AudioDuplex implements AirTubeConnectionCallbackI {
	private AudioService as;
	private AudioClient ac;

	public AudioDuplex() {
		as = new AudioService();
		ac = new AudioClient();
	}

	public AudioDuplex(boolean passive) {
		as = new AudioService();
		ac = new AudioClient(passive);
	}

	@Override
	public void onConnect(AirTubeInterfaceI airtube) {
		as.onConnect(airtube);
		ac.onConnect(airtube);
	}

	@Override
	public void onDisconnect() {
		as.onDisconnect();
		ac.onDisconnect();
	}

	public void start() {
		as.start();
		ac.start();
	}

	public void stop() {
		as.stop();
		ac.stop();
	}

	public void unregister() {
		as.unregister();
		ac.unregister();
	}

	public void subscribeTo(AirTubeID sid) {
		ac.subscribeTo(sid);
	}

	public void unsubscribe() {
		ac.unsubscribe();
	}

	public AirTubeID getServiceId() {
		return as.getServiceId();
	}

	public Statistics getStats() {
		return ac.getStats();
	}

	public void setSpeexAEC(boolean on) {
		ac.setSpeexAEC(on);
		as.setSpeexAEC(on);
	}
}
