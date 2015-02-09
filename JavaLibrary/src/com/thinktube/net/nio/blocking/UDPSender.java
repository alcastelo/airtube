package com.thinktube.net.nio.blocking;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class UDPSender {
	protected DatagramChannel channel;

	public UDPSender() {
		try {
			channel = DatagramChannel.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public UDPSender(DatagramChannel c) {
		this.channel = c;
	}

	public UDPSender(InetAddress ip, int port) {
		this();
		connect(ip, port);
	}

	public UDPSender(String ip, int port) {
		this();
		connect(ip, port);
	}

	public void connect(String ip, int port) {
		connect(new InetSocketAddress(ip, port));
	}

	public void connect(InetAddress ip, int port) {
		connect(new InetSocketAddress(ip, port));
	}

	public void connect(InetSocketAddress isa) {
		try {
			/*
			 * In order to "connect" to the broadcast address we have to
			 * setBroadcast() first
			 */
			byte[] b = isa.getAddress().getAddress();
			if (b[0] == -1 && b[1] == -1 && b[2] == -1 && b[3] == -1) {
				channel.socket().setBroadcast(true);
			}
			channel.connect(isa);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void send(ByteBuffer data) {
		try {
			channel.write(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** Note this only works on non-connected channels */
	public void send(InetSocketAddress ip, ByteBuffer buf) {
		try {
			channel.send(buf, ip);
		} catch (Exception e) {
			//Log.e(TAG, "******* send failed, len " + len);
			e.printStackTrace();
		}
	}

	public void send(ByteBuffer[] bufs) {
		try {
			channel.write(bufs);
		} catch (Exception e) {
			//Log.e(TAG, "******* send failed, len " + len);
			e.printStackTrace();
		}
	}

	public void close() {
		if (channel != null)
			try {
				channel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	public void setTrafficClass(int val) {
		try {
			channel.socket().setTrafficClass(val);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
}
