package com.thinktube.net.nio.blocking;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public interface TCPPacketReceiveCallback {
	boolean handle(ByteBuffer buf, SocketChannel channel);
}
