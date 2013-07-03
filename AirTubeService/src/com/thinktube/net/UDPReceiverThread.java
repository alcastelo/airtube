package com.thinktube.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UDPReceiverThread extends Thread {
	private volatile boolean running = true;
	UDPReceiveCallback recvCallback = null;
	DatagramSocket socket;
	byte[] buffer = new byte[1500];

	public UDPReceiverThread(int port) {
		setDaemon(true);

		try {
			socket = new DatagramSocket(port);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public UDPReceiverThread(DatagramSocket s) {
		setDaemon(true);

		socket = s;
		// socket.setSoTimeout(1000); // block for max 1 sec

	}

	@Override
	public void run() {
		DatagramPacket packet;

		while (running) {

			packet = new DatagramPacket(buffer, buffer.length);

			try {
				socket.receive(packet);

				if (recvCallback != null)
					recvCallback.handlePacket(packet);

			} catch (SocketException e2) {
				if (running)
					e2.printStackTrace();
				// else receive was interupted, and this is ok when we want to
				// stop the thread.
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	public void setReceiveCallback(UDPReceiveCallback cb) {
		recvCallback = cb;
	}

	public void setBufferSize(int size) {
		buffer = new byte[size];
	}

	public void stopRunning() {
		running = false;
		socket.close();
	}
}
