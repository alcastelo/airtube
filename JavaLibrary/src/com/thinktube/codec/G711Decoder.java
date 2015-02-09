package com.thinktube.codec;

import com.thinktube.audio.AudioConfig;
import com.thinktube.audio.G711ACodec;

public class G711Decoder extends Codec implements AudioDecoderI {
	private G711ACodec g711a = new G711ACodec();

	public G711Decoder() {
	}

	@Override
	public void init(AudioConfig ac, byte[] unused) {
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public boolean decode(byte[] input, int offset, int len, short[] output, boolean fec) {
		g711a.decode(output, input, len, offset);
		super.callOutCallback(output, len);
		return true;
	}

	@Override
	public boolean hasPLC() {
		return false;
	}

	@Override
	public boolean hasFEC() {
		return false;
	}
}
