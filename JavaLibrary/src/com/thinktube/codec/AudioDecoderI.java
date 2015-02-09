package com.thinktube.codec;

import com.thinktube.audio.AudioConfig;

public interface AudioDecoderI {
	void init(AudioConfig ac, byte[] configBytes);
	void setCallback(CodecOutCallbackI cb);
	void start();
	void stop();
	boolean decode(byte[] input, int offset, int len, short[] output, boolean fec);
	boolean hasPLC(); /* indicate if codec can do packet loss concealment */
	boolean hasFEC(); /* indicate if codec can do forward error correction */
}
