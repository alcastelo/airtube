package com.thinktube.android.codec;

import java.nio.ByteBuffer;

import android.annotation.TargetApi;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.os.Build;

import com.thinktube.codec.CodecOutCallbackI;
import com.thinktube.codec.CodecOutMulti;

public class CodecOutMultiAndroid extends CodecOutMulti implements CodecOutCallbackAndroidI {
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	public void handleFrame(ByteBuffer buf, BufferInfo info) {
		for (CodecOutCallbackI cb : cbs) {
			if (cb instanceof CodecOutCallbackAndroidI) {
				((CodecOutCallbackAndroidI)cb).handleFrame(buf, info);
			} else {
				// TODO: ignore flags for base class?
				cb.handleFrame(buf, info.size, 0);
			}
			buf.rewind();
		}
	}

	@Override
	public void formatChanged(MediaFormat format) {
		for (CodecOutCallbackI cb : cbs) {
			if (cb instanceof CodecOutCallbackAndroidI) {
				((CodecOutCallbackAndroidI)cb).formatChanged(format);
			}
		}
	}
}
