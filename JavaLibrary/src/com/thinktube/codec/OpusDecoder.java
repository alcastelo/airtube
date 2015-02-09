package com.thinktube.codec;

import java.util.logging.Logger;

import com.thinktube.audio.AudioConfig;
import com.thinktube.jni.OpusDecoderNative;

public class OpusDecoder extends Codec implements AudioDecoderI {
	private final static Logger LOG = Logger.getLogger(OpusDecoder.class.getSimpleName());
	private static final int MIN_FRAME_SIZE = 160;

	private OpusDecoderNative opus = new OpusDecoderNative();
	private AudioConfig ac;

	public OpusDecoder() {
	}

	/** WARNING: may modify AudioConfig.frameSize! */
	@Override
	public void init(AudioConfig ac, byte[] unused) {
		this.ac = ac;
		ac.frameSize = (ac.frameSize/MIN_FRAME_SIZE)*MIN_FRAME_SIZE;
	}

	@Override
	public void start() {
		opus.create(ac.sampleRate, ac.CHANNELS);
	}

	@Override
	public void stop() {
		opus.destroy();
	}

	@Override
	public boolean decode(byte[] input, int offset, int len, short[] output, boolean fec) {
		//long time = System.currentTimeMillis();
		int size = opus.decode(input, offset, len, output, 0, ac.frameSize, fec ? 1 : 0);
		//LOG.fine("Opus decode took " + (System.currentTimeMillis()-time));
		if (size != ac.frameSize) {
			LOG.warning("Opus decoder error!");
			return false;
		}
		super.callOutCallback(output, size);
		return true;
	}

	@Override
	public boolean hasPLC() {
		return true;
	}

	@Override
	public boolean hasFEC() {
		return true;
	}
}
