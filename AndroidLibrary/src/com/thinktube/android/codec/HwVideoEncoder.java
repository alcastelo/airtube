package com.thinktube.android.codec;

import com.thinktube.android.video.PixelFormat;
import com.thinktube.codec.CodecOutCallbackI;
import android.annotation.TargetApi;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

public class HwVideoEncoder extends HwEncoder implements Camera.PreviewCallback {

	private final int width, height, frameRate, bps, iFrameInt;
	byte[] convertedData;
	private Surface mSurface;

	public HwVideoEncoder(CodecOutCallbackI cb, int width, int height, int frameRate, int bps, int iFrameInt) {
		super(cb);
		TAG = "HwVideoEncoder";
		this.width = width;
		this.height = height;
		this.frameRate = frameRate;
		this.bps = bps;
		this.iFrameInt = iFrameInt;

		convertedData = new byte[PixelFormat.getYV12PreviewSize(width, height)];
		expectedInputSize = PixelFormat.getYV12PreviewSize(width, height);
	}

	public Surface getInputSurface() {
		return mSurface;
	}

	@Override
	public void init() {
		init(false);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	public void init(boolean useTexture) {
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
			Log.e(TAG, "MediaCodec not available in this version of Android! Expect errors!");
			return;
		}
		if (useTexture && android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
			Log.e(TAG, "texture not available in this version of Android! Disabled!");
			useTexture = false;
		}
		mediaCodec = codecImpl.getVideoEncoder();

		MediaFormat format;
		format = MediaFormat.createVideoFormat("video/avc", width, height);
		format.setInteger(MediaFormat.KEY_BIT_RATE, bps);
		format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
		if (useTexture) {
			format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
							  MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
		}
		else {
			format.setInteger(MediaFormat.KEY_COLOR_FORMAT, codecImpl.getColorFormat());
		}
		format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInt);

		/* For video sizes which are not a multiple of 16 there are various codec limitations
		 * and it may be necessary to set "stride" and "slice-height".
		 * For now I suggest we just stay with sizes which are multiples of 16
		 * See https://android-review.googlesource.com/#/c/43410/1/tests/tests/media/src/android/media/cts/DecoderTest.java
		 */

		mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		if (useTexture) {
			mSurface = mediaCodec.createInputSurface();
		}
		Log.d(TAG, "initialized with format " + format);
	}

	@Override
	public void input(byte[] data, int offset, int len, int flags) {
		convertedData = codecImpl.cvtPixelFormat(data, convertedData, width, height);
		super.input(convertedData, offset, len, flags);
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera cam) {
		if (ready) {
			input(data);
		}
	}
}
