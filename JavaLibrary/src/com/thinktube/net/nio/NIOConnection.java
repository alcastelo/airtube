package com.thinktube.net.nio;

import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;

/** This is the interface for the NIO selector thread */
public interface NIOConnection {

	void handleRead();

	boolean handleWrite();

	boolean hasData();

	SelectableChannel getChannel();

	InetSocketAddress getRemoteISA();

}
