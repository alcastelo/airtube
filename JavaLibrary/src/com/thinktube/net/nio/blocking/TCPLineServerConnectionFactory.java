package com.thinktube.net.nio.blocking;

import java.nio.channels.SocketChannel;

public class TCPLineServerConnectionFactory implements TCPServerConnectionFactory {
	private TCPLineReceiveCallback cb;

	public TCPLineServerConnectionFactory(TCPLineReceiveCallback cb) {
		this.cb = cb;
	}

	public TCPServerConnection create(SocketChannel sock, TCPServerThread serv) {
		return new TCPLineServerConnection(sock, cb, serv);
	}
}
