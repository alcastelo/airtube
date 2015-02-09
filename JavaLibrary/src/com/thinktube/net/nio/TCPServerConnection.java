package com.thinktube.net.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;

/**
 * This class exists mostly so we can standardize on NIOConnection instead of
 * dealing with Channels directly in NIOSelectorThread. It holds the
 * ServerSocketChannel and the Callback we pass to all client connections.
 *
 * Later we may add statistics and TOS.
 */
public class TCPServerConnection implements NIOConnection {
	private final PktConnectionCallback cb;	// this is the callback we pass to all client connections
	private final ServerSocketChannel chan;

	TCPServerConnection(PktConnectionCallback cb, ServerSocketChannel chan) {
		this.cb = cb;
		this.chan = chan;
	}

	@Override
	public void handleRead() {
		throw new RuntimeException("read not possible on ServerSocket!");
	}

	@Override
	public boolean handleWrite() {
		throw new RuntimeException("write not possible on ServerSocket!");
	}

	@Override
	public boolean hasData() {
		// not used
		return false;
	}

	@Override
	public SelectableChannel getChannel() {
		return chan;
	}

	public PktConnectionCallback getCallback() {
		return cb;
	}

	@Override
	public InetSocketAddress getRemoteISA() {
		// not used
		return null;
	}

	public static void create(NIOSelectorThread selector, int port, PktConnectionCallback cb) throws IOException {
		ServerSocketChannel chan = ServerSocketChannel.open();
		chan.socket().bind(new InetSocketAddress(port));
		chan.configureBlocking(false);

		//TODO: We can't set ToS on a ServerSocket, but we would like to set the TOS of the SocketChannel after accept

		TCPServerConnection conn = new TCPServerConnection(cb, chan);
		selector.requestRegister(conn, SelectionKey.OP_ACCEPT);
		// this calls selector.wakeup();
	}
}
