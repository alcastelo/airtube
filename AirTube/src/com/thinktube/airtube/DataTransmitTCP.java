package com.thinktube.airtube;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.thinktube.net.NetworkInterfaces;
import com.thinktube.net.nio.NIOSelectorThread;
import com.thinktube.net.nio.PktConnectionCallback;
import com.thinktube.net.nio.PktConnection;
import com.thinktube.net.nio.TCPConnection;
import com.thinktube.net.nio.TCPServerConnection;

public class DataTransmitTCP implements DataTransmitI {
	private static final int PORT = 9992;
	private final NIOSelectorThread selector;

	public DataTransmitTCP(NIOSelectorThread selector) {
		this.selector = selector;
	}

	@Override
	public void start(PktConnectionCallback cb, NetworkInterfaces unused_nifs) throws IOException {
		TCPServerConnection.create(selector, PORT, cb);
	}

	@Override
	public void stop() {
	}

	@Override
	public PktConnection createConnection(InetAddress to, TrafficClass tos, PktConnectionCallback cb) throws IOException {
		return TCPConnection.create(selector, new InetSocketAddress(to, PORT), tos.getTOSValue(), cb);
	}
}
