package com.thinktube.airtube.android;

import java.util.logging.Logger;

import com.thinktube.airtube.*;
import com.thinktube.android.audio.AudioRecorder;
import com.thinktube.audio.AudioConfig;
import com.thinktube.codec.AudioCodecFactory;
import com.thinktube.codec.AudioEncoderI;
import com.thinktube.codec.AudioProtocol;
import com.thinktube.codec.ProtocolSenderI;

public class AudioService extends AirTubeComponent {
	private final static Logger LOG = Logger.getLogger(AudioService.class.getSimpleName());
	private static final AudioCodecFactory.Codec CODEC = AudioCodecFactory.Codec.OPUS;

	private ServiceDescription myServ = new ServiceDescription("audio", TransmissionType.UDP, TrafficClass.VOICE);
	private AirTubeInterfaceI airtube;
	private AirTubeID myId;

	private final AudioRecorder audioRec;
	private int subsCount = 0;
	private AudioConfig audioConf;

	public AudioService() {
		AudioProtocol.OutCallback outCb = new AudioProtocol.OutCallback(new ProtocolSenderI() {
			@Override
			public void sendPacket(byte[] bytes, int len) {
				ServiceDataParcel sd = new ServiceDataParcel(bytes, len);
				airtube.sendServiceData(myId, sd);
			}
		});

		audioConf = new AudioConfig(8000, 20, true);
		AudioEncoderI enc = AudioCodecFactory.getEncoder(CODEC);
		enc.init(audioConf, 32000, outCb);
		audioRec = new AudioRecorder(enc, audioConf);
	}

	@Override
	public void onConnect(AirTubeInterfaceI airtube) {
		this.airtube = airtube;
		register();
	}

	@Override
	public void onDisconnect() {
		stop();
	}

	@Override
	public void onSubscription(AirTubeID sid, AirTubeID cid, ConfigParameters config) {
		LOG.info("onSubscription " + cid);
		if (subsCount++ == 0) {
			start();
		}
	}

	@Override
	public void onUnsubscription(AirTubeID sid, AirTubeID cid) {
		LOG.info("onUnSubscription " + cid);
		if (--subsCount == 0) {
			stop();
		}
	}

	private void register() {
		myServ.config = new ConfigParameters();
		myServ.config.put("codec", CODEC.toString());
		myServ.config.put("sample-rate", audioConf.sampleRate);
		myServ.config.put("frame-size", audioConf.frameSize);
		byte[] cb = audioRec.getConfigBytes();
		if (cb != null) {
			myServ.config.put("config-bytes", cb);
		}
		myId = airtube.registerService(myServ, this);
		LOG.info("registered myself as " + myId);
	}

	@Override
	public void start() {
		audioRec.start();
	}

	@Override
	public void stop() {
		audioRec.stop();
	}

	@Override
	public void unregister() {
		airtube.unregisterService(myId);
	}

	public AirTubeID getServiceId() {
		return myId;
	}

	public void setSpeexAEC(boolean on) {
		audioRec.setSpeexAEC(on);
	}
}
