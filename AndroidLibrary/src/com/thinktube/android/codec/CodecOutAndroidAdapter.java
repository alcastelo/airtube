package com.thinktube.android.codec;

import java.nio.ByteBuffer;

import com.thinktube.codec.Codec;
import com.thinktube.codec.CodecOutCallbackI;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class CodecOutAndroidAdapter implements CodecOutCallbackAndroidI {
	private final CodecOutCallbackI cb;

	public CodecOutAndroidAdapter(CodecOutCallbackI cb) {
		this.cb = cb;
	}

	@Override
	public void handleFrame(ByteBuffer buf, BufferInfo info) {
		int flags = 0;
		if ((info.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0) {
			flags = Codec.FLAG_SYNC_FRAME;
		}
		if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
			flags |= Codec.FLAG_CODEC_CONFIG;
		}
		cb.handleFrame(buf, info.size, flags);
	}

	@Override
	public void formatChanged(MediaFormat format) {
		// not available in generic CodecOutCallbackI
	}

	@Override
	public void handleFrame(byte[] buf, int size) {
		cb.handleFrame(buf, size);
	}

	@Override
	public void handleFrame(short[] buf, int size) {
		cb.handleFrame(buf, size);
	}

	@Override
	public void handleFrame(ByteBuffer buf, int size, int flags) {
		cb.handleFrame(buf, size, flags);
	}

	@Override
	public void stop() {
		cb.stop();
	}

	@Override
	public int getNecessaryHeadroom() {
		return cb.getNecessaryHeadroom();
	}
}
