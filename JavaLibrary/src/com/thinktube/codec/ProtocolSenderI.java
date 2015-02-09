package com.thinktube.codec;

public interface ProtocolSenderI {
	void sendPacket(byte[] bytes, int len);
}
