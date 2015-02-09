package com.thinktube.codec;

import com.thinktube.audio.AudioConfig;
import com.thinktube.jni.Speex;

public class SpeexEncoder extends Codec implements AudioEncoderI {
	private static final int MIN_FRAME_SIZE = 160;

	private Speex speex = new Speex();
	private int speexQuality;
	private int frameSize = MIN_FRAME_SIZE;
	private byte[] bytes;
	private int outOffset;

	public SpeexEncoder(int quality) {
		this.speexQuality = quality;
	}

	/** WARNING: may modify AudioConfig.frameSize! */
	@Override
	public void init(AudioConfig ac, int bitrate, CodecOutCallbackI cb) {
		super.setCallback(cb);
		outOffset = cb.getNecessaryHeadroom();
		ac.frameSize = (ac.frameSize/MIN_FRAME_SIZE)*MIN_FRAME_SIZE;
		frameSize = ac.frameSize;
		bytes = new byte[frameSize];
	}

	@Override
	public void start() {
		speex.encoderCreate(speexQuality);
	}

	@Override
	public void stop() {
		speex.encoderDestroy();
	}

	@Override
	public void input(short[] data) {
		int size = speex.encode(data, 0, frameSize, bytes, outOffset, frameSize-outOffset);
		if (size < 0) {
			return;
		}
		super.callOutCallback(bytes, size);
	}

	@Override
	public byte[] getConfigBytes() {
		return null;
	}
}
