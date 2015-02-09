package com.thinktube.codec;

import com.thinktube.audio.AudioConfig;

public interface AudioEncoderI {
	void init(AudioConfig ac, int bitrate, CodecOutCallbackI cb);
	void start();
	void stop();
	void input(short[] input);
	byte[] getConfigBytes(); /* necessary only for AAC */
}
