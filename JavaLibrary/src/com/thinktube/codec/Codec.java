package com.thinktube.codec;

/**
 * The base class for all our codecs, encoders and decoders.
 * Should it be called Call-back-ing codec?
 *
 * All codecs have the option to register a callback which is called every time
 * the codec outputs something. Subclasses have to take care to call the superclass
 * functions for this to happen.
 *
 * @author br1
 */
public class Codec {
	public final static byte FLAG_SYNC_FRAME = 0x01;
	public final static byte FLAG_CODEC_CONFIG = 0x02;

	protected CodecOutCallbackI outCb;

	protected Codec() {
	}

	protected Codec(CodecOutCallbackI cb) {
		outCb = cb;
	}

	public void setCallback(CodecOutCallbackI cb) {
		outCb = cb;
	}

	protected void callOutCallback(byte[] buf, int size) {
		if (outCb != null) {
			outCb.handleFrame(buf, size);
		}
	}
	
	protected void callOutCallback(short[] buf, int size) {
		if (outCb != null) {
			outCb.handleFrame(buf, size);
		}
	}
	
	protected void callbackStop() {
		if (outCb != null) {
			outCb.stop();
		}
	}
}
