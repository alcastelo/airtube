package com.thinktube.net.nio.blocking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;

public class UDPReceiverThread extends Thread {
	private volatile boolean running = true;
	UDPReceiveCallback recvCallback = null;
	DatagramChannel channel;
	ByteBuffer buffer;

	public UDPReceiverThread(int port) {
		setDaemon(true);
		setName("UDP Receiver");

		try {
			channel = DatagramChannel.open();
			channel.socket().setReuseAddress(true);
			channel.socket().bind(new InetSocketAddress(port));
		} catch (IOException e) {
			e.printStackTrace();
		}
		buffer = ByteBuffer.allocate(1500);
	}

	public UDPReceiverThread(DatagramChannel c) {
		setDaemon(true);

		channel = c;
		buffer = ByteBuffer.allocate(1500);
		// socket.setSoTimeout(1000); // block for max 1 sec

	}

	InetSocketAddress add;

	@Override
	public void run() {
		while (running) {
			try {
				buffer.clear();
				add = (InetSocketAddress)channel.receive(buffer);
				buffer.flip();
				if (recvCallback != null)
					recvCallback.handlePacket(buffer, add);
			} catch (ClosedChannelException e2) {
				if (running)
					e2.printStackTrace();
				// else receive was interupted, and this is ok when we want to
				// stop the thread.
				break;
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	public void setReceiveCallback(UDPReceiveCallback cb) {
		recvCallback = cb;
	}

	public void setBufferSize(int size) {
		buffer = ByteBuffer.allocate(size);
	}

	public void stopRunning() {
		running = false;
		try {
			channel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
