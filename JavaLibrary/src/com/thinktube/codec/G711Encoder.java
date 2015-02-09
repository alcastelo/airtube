package com.thinktube.codec;

import com.thinktube.audio.AudioConfig;
import com.thinktube.audio.G711ACodec;

public class G711Encoder extends Codec implements AudioEncoderI {
	private G711ACodec g711a = new G711ACodec();
	private int frameSize;
	private byte[] bytes;
	private int outOffset;

	public G711Encoder() {
	}

	@Override
	public void init(AudioConfig ac, int bitrate, CodecOutCallbackI cb) {
		super.setCallback(cb);
		outOffset = cb.getNecessaryHeadroom();
		frameSize = ac.frameSize;
		bytes = new byte[frameSize + outOffset];
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public void input(short[] data) {
		g711a.encode(data, frameSize, bytes, outOffset);
		super.callOutCallback(bytes, frameSize);
	}

	@Override
	public byte[] getConfigBytes() {
		return null;
	}
}
