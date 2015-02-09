package com.thinktube.codec;

import com.thinktube.audio.AudioConfig;
import com.thinktube.jni.Speex;

public class SpeexDecoder extends Codec implements AudioDecoderI {
	private static final int MIN_FRAME_SIZE = 160;

	private Speex speex = new Speex();
	private int frameSize = MIN_FRAME_SIZE;

	public SpeexDecoder() {
	}

	/** WARNING: may modify AudioConfig.frameSize! */
	@Override
	public void init(AudioConfig ac, byte[] unused) {
		ac.frameSize = (ac.frameSize/MIN_FRAME_SIZE)*MIN_FRAME_SIZE;
		frameSize = ac.frameSize;
	}

	@Override
	public void start() {
		speex.decoderCreate();
	}

	@Override
	public void stop() {
		speex.decoderDestroy();
	}

	@Override
	public boolean decode(byte[] input, int offset, int len, short[] output, boolean fec) {
		int size = speex.decode(input, offset, len, output, 0, frameSize);
		if (size != frameSize) {
			return false;
		}
		super.callOutCallback(output, size);
		return true;
	}

	@Override
	public boolean hasPLC() {
		// The codec has it but it's not implemented in JNI yet
		return false;
	}

	@Override
	public boolean hasFEC() {
		return false;
	}
}
