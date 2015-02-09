package com.thinktube.android.codec;

import java.nio.ByteBuffer;
import com.thinktube.codec.CodecOutCallbackI;
import android.annotation.TargetApi;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class HwVideoDecoder extends HwDecoder {
	private int width, height;

	public HwVideoDecoder(int width, int height) {
		this(null, width, height);
	}

	public HwVideoDecoder(CodecOutCallbackI cb, int width, int height) {
		super(cb);
		TAG = "HwVideoDecoder";
		this.width = width;
		this.height = height;
		timeout = 20000;
	}

	/* note that if surface is provided, callbacks can't be used */
	public void init(byte[] configBytes, Surface renderSurface)
	{
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
			Log.e(TAG, "MediaCodec not available in this version of Android! Expect errors!");
			return;
		}

		this.configBytes = configBytes;
		mediaCodec = codecImpl.getVideoDecoder();

		MediaFormat format;
		format = MediaFormat.createVideoFormat("video/avc", width, height);
		// format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 65535);		// test

		if (configBytes != null)
			format.setByteBuffer("csd-0", ByteBuffer.wrap(configBytes));

		mediaCodec.configure(format, renderSurface, null, 0);
		Log.d(TAG, "initialized with format " + format + (renderSurface != null ? " and Surface" : ""));
	}

	@Override
	public void init(byte[] configBytes) {
		init(configBytes, null);
	}
}
