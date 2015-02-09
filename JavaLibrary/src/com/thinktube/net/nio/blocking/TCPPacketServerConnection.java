package com.thinktube.net.nio.blocking;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

class TCPPacketServerConnection extends TCPServerConnection {
	TCPPacketReceiveCallback recvCb;
	ByteBuffer len = ByteBuffer.allocate(2);
	ByteBuffer buf = ByteBuffer.allocateDirect(1500);
	short expectedLen = 0;

	public TCPPacketServerConnection(SocketChannel c, TCPPacketReceiveCallback cb, TCPServerThread serv) {
		super(c, serv);
		recvCb = cb;
	}

	public synchronized boolean handle() throws IOException {
		int c;
		if (expectedLen == 0) {
			c = channel.read(len);
			if (c == -1) { // EOF, socket closed
				System.out.println("closed, len");
				return false;
			}
			if (len.position() == 2) {
				len.flip();
				expectedLen = len.getShort();
				len.clear();
				buf.limit(expectedLen);
			}
		} else {
			c = channel.read(buf);
			if (buf.position() == expectedLen) {
				buf.flip();
				recvCb.handle(buf, channel);
				buf.clear();
				expectedLen = 0;
			}
			if (c == -1) { // EOF, socket closed
				System.out.println("closed, buf");
				return false;
			}
		}
		return true;
	}

	public void close() {
		//Log.d(TAG, "*** closing");
	}
}
