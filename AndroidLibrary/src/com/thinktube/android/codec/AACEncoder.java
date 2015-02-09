package com.thinktube.android.codec;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import com.thinktube.audio.AudioConfig;
import com.thinktube.codec.AudioEncoderI;
import com.thinktube.codec.CodecOutCallbackI;
import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class AACEncoder extends HwEncoder implements AudioEncoderI {
	AudioConfig ac;
	private int bitRate;
	private byte[] buffer;
	private ShortBuffer sb;

	public AACEncoder() {
		this(null);
	}

	public AACEncoder(CodecOutCallbackI cb) {
		super(cb);
		TAG = "AACEncoder";

		/* the AAC ELD codec always outputs data in same size chunks (bitrate and profile specific).
		 * It expects input of 512 frames (with 16bit samples this is 1024 byte), if not it will not
		 * provide output for every input frame, and we should set useThread = true. We take care in
		 * AudioRecorder to input 1024 bytes so we don't have to use a thread. */

		expectedInputSize = 1024;
	}

	@Override
	public void init(AudioConfig ac, int bitrate, CodecOutCallbackI cb) {
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
			Log.e(TAG, "MediaCodec not available in this version of Android! Expect errors!");
			return;
		}
		super.setCallback(cb);
		ac.frameSize = 512;	/* AAC-ELD only works with 512 */
		this.ac = ac;
		this.bitRate = bitrate;
		buffer = new byte[1024];
		sb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
		init();
	}

	@Override
	public void init()
	{
		mediaCodec = codecImpl.getAudioEncoder();

		MediaFormat format;
		format = MediaFormat.createAudioFormat("audio/mp4a-latm", ac.sampleRate, ac.CHANNELS);
		format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
		/* ELD is Enhanced Low Delay */
		format.setInteger(MediaFormat.KEY_AAC_PROFILE,
				MediaCodecInfo.CodecProfileLevel.AACObjectELD);

		mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		Log.d(TAG, "initialized with format " + format);
	}

	@Override
	public void input(short[] input) {
		sb.clear();
		sb.put(input);
		super.input(buffer);
	}
}
