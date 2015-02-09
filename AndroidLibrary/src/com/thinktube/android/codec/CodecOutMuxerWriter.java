package com.thinktube.android.codec;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class CodecOutMuxerWriter implements CodecOutCallbackAndroidI {
	private static final String TAG = "MuxerWriter";
	private String outputPath = new File(
			Environment.getExternalStorageDirectory(), "test.mp4").toString();
	private MediaMuxer mMuxer;
	private int mTrackIndex = -1;
	private int framesOut = 0;
	private int frameTimeUs =  1000000 / 15;

	public CodecOutMuxerWriter() {
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
			Log.e(TAG, "MediaCodec not available in this version of Android! Expect errors!");
		}
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
			Log.e(TAG, "feature not available in this version of Android! Expect errors!");
		}
	}

	@Override
	public void handleFrame(ByteBuffer buf, BufferInfo info) {
		if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0
			 && mMuxer != null && mTrackIndex != -1) {
			info.presentationTimeUs = framesOut++ * frameTimeUs;
			mMuxer.writeSampleData(mTrackIndex, buf, info);
		}
	}

	@Override
	public void handleFrame(ByteBuffer buf, int size, int flags) {
		// not used
	}

	@Override
	public void handleFrame(byte[] buf, int size) {
		// not used by video codecs so far
	}

	@Override
	public void handleFrame(short[] buf, int size) {
		// not used by video codecs so far
	}

	@Override
	public void formatChanged(MediaFormat format) {
		try {
			Log.d(TAG, "add track" + format);
			mMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
			mTrackIndex = mMuxer.addTrack(format);
			mMuxer.start();
		} catch (IOException ioe) {
			throw new RuntimeException("MediaMuxer creation failed", ioe);
		}
	}

	@Override
	public void stop() {
		Log.d(TAG, "stop");
		if (mMuxer == null)
			return;
		mMuxer.stop();
		mMuxer.release();
		mMuxer = null;
	}

	public int getNecessaryHeadroom() {
		return 0;
	}
}
