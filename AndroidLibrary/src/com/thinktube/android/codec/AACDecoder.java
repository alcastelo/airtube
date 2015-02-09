package com.thinktube.android.codec;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import com.thinktube.audio.AudioConfig;
import com.thinktube.codec.AudioDecoderI;
import com.thinktube.codec.CodecOutCallbackI;
import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class AACDecoder extends HwDecoder implements AudioDecoderI {
	private AudioConfig audioConf;
	ByteBuffer outBB;

	public AACDecoder() {
		this(null);
	}

	public AACDecoder(CodecOutCallbackI cb) {
		super(cb);
		TAG = "AACDecoder";
	}

	@Override
	public void init(AudioConfig ac, byte[] configBytes) {
		ac.frameSize = 512;
		outBB = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);
		this.audioConf = ac;
		init(configBytes);
	}

	@Override
	public void init(byte[] configBytes) {
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
			Log.e(TAG, "MediaCodec not available in this version of Android! Expect errors!");
			return;
		}
		this.configBytes = configBytes;
		mediaCodec = codecImpl.getAudioDecoder();

		MediaFormat format;
		format = MediaFormat.createAudioFormat("audio/mp4a-latm", audioConf.sampleRate, audioConf.CHANNELS);

		if (configBytes != null)
			format.setByteBuffer("csd-0", ByteBuffer.wrap(configBytes));

		mediaCodec.configure(format, null, null, 0);
		Log.d(TAG, "initialized with format " + format);
	}

	@Override
	public void start() {
		super.start();
		initialInput();
	}

	public void initialInput() {
		Log.d(TAG, "initial input of config bytes: " + configBytes.length);
		try {
			for (int i=0; i < 5; i++) {
				int index = mediaCodec.dequeueInputBuffer(50000);
				if (index < 0) {
					Log.d(TAG, "*** getConfig: dropped input frame");
					return;
				}
				inputBuffers[index].clear();
				inputBuffers[index].put(configBytes);
				mediaCodec.queueInputBuffer(index, 0, configBytes.length, 100, 0);

				int res = output(50000, null);
				if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
					break;
				}
			}
			Log.d(TAG, "initital input finished");
		} catch (IllegalStateException e) {
			Log.d(TAG, "getConfig: illegal codec state");
		}
	}

	@Override
	public boolean decode(byte[] input, int offset, int len, short[] output, boolean fec) {
		//long time = System.currentTimeMillis();
		_input(input, offset, len, 0);
		int ret = output(-1, outBB);
		//Log.d(TAG, "Decode took " + (System.currentTimeMillis()-time));
		outBB.asShortBuffer().get(output);
		return (ret >= 0);
	}

	@Override
	public boolean hasPLC() {
		// The codec itself may have it but I dont know how to activate it
		return false;
	}

	@Override
	public boolean hasFEC() {
		return false;
	}
}
