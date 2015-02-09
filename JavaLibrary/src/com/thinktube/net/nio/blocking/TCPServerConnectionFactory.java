package com.thinktube.net.nio.blocking;

import java.nio.channels.SocketChannel;

public interface TCPServerConnectionFactory {
	TCPServerConnection create(SocketChannel sock, TCPServerThread serv);
}
