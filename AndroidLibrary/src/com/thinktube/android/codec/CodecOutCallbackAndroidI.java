package com.thinktube.android.codec;

import java.nio.ByteBuffer;

import com.thinktube.codec.CodecOutCallbackI;

import android.media.MediaCodec;
import android.media.MediaFormat;

/** Android specific callback for MediaFormat and BufferInfo */
public interface CodecOutCallbackAndroidI extends CodecOutCallbackI {
	void handleFrame(ByteBuffer buf, MediaCodec.BufferInfo info);
	void formatChanged(MediaFormat format);
}
