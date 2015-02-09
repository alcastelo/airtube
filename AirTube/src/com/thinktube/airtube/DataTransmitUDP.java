package com.thinktube.airtube;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.thinktube.net.NetworkInterfaces;
import com.thinktube.net.nio.NIOSelectorThread;
import com.thinktube.net.nio.PktConnectionCallback;
import com.thinktube.net.nio.PktConnection;
import com.thinktube.net.nio.UDPConnection;

public class DataTransmitUDP implements DataTransmitI {
	private static final int PORT = 9992;
	private final NIOSelectorThread selector;
	private PktConnection recvConn;

	public DataTransmitUDP(NIOSelectorThread selector) {
		this.selector = selector;
	}

	@Override
	public void start(PktConnectionCallback cb, NetworkInterfaces unused_nifs) throws IOException {
		recvConn = UDPConnection.createReceiver(selector, new InetSocketAddress(PORT), cb);
	}

	@Override
	public void stop() {
		if (recvConn != null) {
			recvConn.close();
		}
	}

	@Override
	public PktConnection createConnection(InetAddress to, TrafficClass tos, PktConnectionCallback cb) throws IOException {
		return UDPConnection.createSender(selector, new InetSocketAddress(to, PORT), tos.getTOSValue(), false, cb);
	}
}
