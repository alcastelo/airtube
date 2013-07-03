package com.thinktube.net;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.util.Log;

public class UDPSender {
	private static final String TAG = "UDPSender";
	protected DatagramSocket socket;
	protected InetAddress serverAddr;
	protected int mPort;

	public UDPSender() {
		try {
			socket = new DatagramSocket();
			// socket.setSendBufferSize(262143); // default is 110592 max 262142
			// Log.d(TAG, "send buffer size: " + socket.getSendBufferSize());
		} catch (Exception e) {
			e.printStackTrace();
			socket = null;
		}
	}

	public UDPSender(int port) {
		this();
		mPort = port;
	}

	public UDPSender(String ip, int port) {
		this();
		setTarget(ip, port);
	}

	public void setTarget(String ip, int port) {
		mPort = port;
		try {
			serverAddr = InetAddress.getByName(ip);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			serverAddr = null;
		}
	}

	public void setTarget(InetAddress ip, int port) {
		mPort = port;
		serverAddr = ip;
	}

	public void send(byte[] data) {
		send(serverAddr, data, data.length);
	}

	public void send(InetAddress ip, byte[] data) {
		send(ip, data, data.length);
	}

	public void send(InetAddress ip, byte[] data, int len) {
		try {
			DatagramPacket packet = new DatagramPacket(data, len, ip, mPort);
			socket.send(packet);
		} catch (Exception e) {
			Log.e(TAG, "******* send failed, len " + len);
			e.printStackTrace();
		}
	}

	public boolean senderReady() {
		return (socket != null && serverAddr != null);
	}

	public void close() {
		if (socket != null)
			socket.close();
	}

	public void setTrafficClass(int val) {
		try {
			socket.setTrafficClass(val);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
