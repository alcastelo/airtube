package com.thinktube.codec;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class CodecOutMulti implements CodecOutCallbackI {
	protected List<CodecOutCallbackI> cbs = new ArrayList<CodecOutCallbackI>();

	public CodecOutMulti() {
	}

	public void handleFrame(byte[] buf, int size) {
		for (CodecOutCallbackI cb : cbs) {
			cb.handleFrame(buf, size);
		}
	}

	public void handleFrame(short[] buf, int size) {
		for (CodecOutCallbackI cb : cbs) {
			cb.handleFrame(buf, size);
		}
	}

	public void handleFrame(ByteBuffer buf, int size, int flags) {
		for (CodecOutCallbackI cb : cbs) {
			System.out.println("JJJ");
			cb.handleFrame(buf, size, flags);
			buf.rewind();
		}
	}

	@Override
	public void stop() {
		for (CodecOutCallbackI cb : cbs) {
			cb.stop();
		}
	}

	public int getNecessaryHeadroom() {
		int max = 0;
		for (CodecOutCallbackI cb : cbs) {
			if (cb.getNecessaryHeadroom() > max) {
				max = cb.getNecessaryHeadroom();
			}
		}
		return max;
	}

	public void addCb(CodecOutCallbackI cb) {
		cbs.add(cb);
	}
}
