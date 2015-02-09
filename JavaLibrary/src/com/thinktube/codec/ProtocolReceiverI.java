package com.thinktube.codec;

public interface ProtocolReceiverI {
	void receiveFrame(byte[] buf, int start, int len, int flags);
	boolean isReady();
}
