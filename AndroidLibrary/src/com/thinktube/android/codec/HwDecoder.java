package com.thinktube.android.codec;

import com.thinktube.codec.CodecOutCallbackI;

public abstract class HwDecoder extends HwCodec {
	protected HwDecoder(CodecOutCallbackI cb) {
		super(cb);
	}

	public abstract void init(byte[] configBytes);
}
