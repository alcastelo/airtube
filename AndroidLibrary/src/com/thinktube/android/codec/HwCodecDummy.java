package com.thinktube.android.codec;

import com.thinktube.codec.CodecOutCallbackI;

public class HwCodecDummy extends HwCodec {
	public HwCodecDummy(CodecOutCallbackI cb) {
		super(cb);
	}

	public void init() {
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public void input(byte[] data, int offset, int len, int flags) {
		super.callOutCallback(data, len);
	}

	@Override
	public boolean isReady() {
		return true;
	}
}
