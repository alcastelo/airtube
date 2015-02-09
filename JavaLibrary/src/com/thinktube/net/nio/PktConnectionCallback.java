package com.thinktube.net.nio;

import java.net.InetAddress;
import java.nio.ByteBuffer;

public interface PktConnectionCallback {
	void handleReceive(ByteBuffer buf, InetAddress from, PktConnection handler);
}
