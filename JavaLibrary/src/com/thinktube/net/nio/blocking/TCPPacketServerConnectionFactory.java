package com.thinktube.net.nio.blocking;

import java.nio.channels.SocketChannel;

public class TCPPacketServerConnectionFactory implements TCPServerConnectionFactory {
	private TCPPacketReceiveCallback cb;

	public TCPPacketServerConnectionFactory(TCPPacketReceiveCallback cb) {
		this.cb = cb;
	}

	public TCPServerConnection create(SocketChannel sock, TCPServerThread serv) {
		return new TCPPacketServerConnection(sock, cb, serv);
	}
}
