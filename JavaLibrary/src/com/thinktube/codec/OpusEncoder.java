package com.thinktube.codec;

import java.util.logging.Logger;

import com.thinktube.audio.AudioConfig;
import com.thinktube.jni.OpusEncoderNative;

public class OpusEncoder extends Codec implements AudioEncoderI {
	private final static Logger LOG = Logger.getLogger(OpusEncoder.class.getSimpleName());
	private OpusEncoderNative opus = new OpusEncoderNative();
	private byte[] bytes;
	private AudioConfig ac;
	private int quality;
	private int outOffset;
	private int bitrate;

	public OpusEncoder(int quality) {
		this.quality = quality;
	}

	/** WARNING: may modify AudioConfig.frameSize! */
	@Override
	public void init(AudioConfig ac, int bitrate, CodecOutCallbackI cb) {
		super.setCallback(cb);
		this.outOffset = cb.getNecessaryHeadroom();
		this.ac = ac;
		this.bitrate = bitrate;

		/* Opus supports 2.5, 5, 10, 20, 40 and 60 ms */
		//TODO: this is not correct for sample rates other than 8000!!!
		int MIN_FRAME_SIZE = 160;
		ac.frameSize = (ac.frameSize/MIN_FRAME_SIZE)*MIN_FRAME_SIZE;

		/* the encoded output is actually independent from MIN_FRAME_SIZE but we can assume it will be smaller */
		bytes = new byte[ac.frameSize];
	}

	@Override
	public void start() {
		int res;

		res = opus.create(ac.sampleRate, ac.CHANNELS, OpusEncoderNative.OPUS_APPLICATION_VOIP);
		if (res != 0) {
			LOG.severe("Could not initialize codec!");
			return;
		}

		opus.setComplexity(quality);
		opus.setSignal(OpusEncoderNative.OPUS_SIGNAL_VOICE);
		/*
		For a frame size of 20 ms, these are the bitrate "sweet spots" for
		Opus in various configurations:
		   o  8-12 kb/s for NB speech,
		   o  16-20 kb/s for WB speech,
		   o  28-40 kb/s for FB speech,
		   o  48-64 kb/s for FB mono music, and
		   o  64-128 kb/s for FB stereo music.
		*/
		opus.setBitrate(bitrate);
		opus.setLsbDepth(16);
		//opus.setInbandFEC(1);
		//opus.setPacketLossPercentage(30);
		//opus.setBitrate(32000);

		LOG.fine("Opus Complexity: " + opus.getComplexity());
		LOG.fine("Opus Signal: " + opus.getSignal());
		LOG.fine("Opus Delay: " + opus.getLookahead());
		LOG.fine("Opus Bitrate: " + opus.getBitrate());
		LOG.fine("Opus FEC: " + opus.getInbandFEC());
		LOG.fine("Opus Loss%: " + opus.getPacketLossPercentage());
	}

	@Override
	public void stop() {
		opus.destroy();
	}

	@Override
	public void input(short[] data) {
		//long time = System.currentTimeMillis();
		int size = opus.encode(data, 0, ac.frameSize, bytes, outOffset, ac.frameSize-outOffset);
		//LOG.fine("Opus encode took " + (System.currentTimeMillis()-time));
		if (size < 0) {
			LOG.warning("Opus encoder error! " + size);
			return;
		}
		if (size == 1) {
			return;
		}
		super.callOutCallback(bytes, size);
	}

	@Override
	public byte[] getConfigBytes() {
		return null;
	}
}
