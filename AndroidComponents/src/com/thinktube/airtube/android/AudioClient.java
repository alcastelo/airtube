package com.thinktube.airtube.android;

import java.util.logging.Logger;

import com.thinktube.airtube.*;
import com.thinktube.android.audio.AudioReceiver;
import com.thinktube.audio.AudioConfig;
import com.thinktube.audio.JitterBuffer.Statistics;
import com.thinktube.codec.AudioCodecFactory;
import com.thinktube.codec.AudioDecoderI;

public class AudioClient extends AirTubeComponent {
	private final static Logger LOG = Logger.getLogger(AudioClient.class.getSimpleName());
	ServiceDescription myServ = new ServiceDescription("audio");
	private AudioReceiver audioRecv;
	private AirTubeID serviceId;
	private AirTubeID myId;
	private AirTubeInterfaceI airtube;
	boolean passive; // this indicates the client should not subscribe by itself as it will be managed from outside
	byte[] configBytes;
	private boolean subscribed;

	public AudioClient(boolean passive) {
		this.passive = passive;
	}

	public AudioClient() {
		this(false);
	}

	@Override
	public void onConnect(AirTubeInterfaceI airtube) {
		this.airtube = airtube;
		register();
	}

	@Override
	public void onDisconnect() {
		myId = null;
		stop();
	}

	@Override
	public void receiveData(AirTubeID sid, ServiceData data) {
		audioRecv.inputData(data.data);
	}

	@Override
	public void onSubscription(AirTubeID serv, AirTubeID id, ConfigParameters conf) {
		LOG.info("onSubscription " + serv);
		subscribed = true;
		start();
	}

	@Override
	public void onUnsubscription(AirTubeID serv, AirTubeID id) {
		LOG.info("onUnSubscription " + serv);
		subscribed = false;
		stop();
	}

	@Override
	public void onServiceFound(AirTubeID id, ServiceDescription desc) {
		if (subscribed) {
			LOG.info("already subscribed to an audio service");
			return;
		}

		if (id.deviceId == myId.deviceId) {
			return;
		}

		LOG.info("service found, subscribing to " + desc);
		serviceId = id;

		configRecv(desc.config);

		airtube.subscribeService(id, myId, null);
	}

	private void configRecv(ConfigParameters config) {
		int sampleRate, frameSize;
		String codec;

		try {
			codec = config.getString("codec");
			sampleRate = config.getInt("sample-rate");
			frameSize = config.getInt("frame-size");
		} catch (Exception e) {
			LOG.info("config parameter missing");
			return;
		}

		try {
			configBytes = config.getBytes("config-bytes");
		} catch (Exception e) {
			// this is OK, most decoders does not need it (just AAC)
		}

		AudioConfig audioConf = new AudioConfig(sampleRate, frameSize);
		AudioDecoderI dec = AudioCodecFactory.getDecoder(AudioCodecFactory.Codec.valueOf(codec));
		dec.init(audioConf, configBytes);
		//dec.setCallback(new com.thinktube.android.codec.CodecOutRawFileWriter("opus.raw"));
		audioRecv = new AudioReceiver(dec, audioConf);
	}

	@Override
	public void start() {
		audioRecv.start();
		if (!subscribed) {
			airtube.subscribeService(serviceId, myId, null);
		}
	}

	@Override
	public void stop() {
		if (subscribed && myId != null) {
			airtube.unsubscribeService(serviceId, myId);
			/* this will trigger below in onUnsubscribe() */
		} else if (audioRecv != null) {
			audioRecv.stop();
		}
	}

	public void register() {
		myId = airtube.registerClient(this);
		if (!passive)
			airtube.findServices(myServ, myId);
	}

	@Override
	public void unregister() {
		if (myId != null) {
			if (!passive)
				airtube.unregisterServiceInterest(myServ, myId);
		}
		if (myId != null && serviceId != null) {
			airtube.unsubscribeService(serviceId, myId);
			serviceId = null;
		}
		if (myId != null) {
			airtube.unregisterClient(myId);
			myId = null;
		}
	}

	public void subscribeTo(AirTubeID sid) {
		serviceId = sid;
		ServiceDescription sd = airtube.getDescription(sid);
		configRecv(sd.config);
		airtube.subscribeService(sid, myId, null);
	}

	public void unsubscribe() {
		if (subscribed && myId != null) {
			airtube.unsubscribeService(serviceId, myId);
		}
	}

	public Statistics getStats() {
		if (audioRecv == null)
			return null;
		return audioRecv.getStats();
	}

	public void setSpeexAEC(boolean on) {
		audioRecv.setSpeexAEC(on);
	}
}
