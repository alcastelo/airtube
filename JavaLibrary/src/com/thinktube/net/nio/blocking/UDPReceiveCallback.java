package com.thinktube.net.nio.blocking;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public interface UDPReceiveCallback {
	void handlePacket(ByteBuffer buffer, InetSocketAddress from);
}
