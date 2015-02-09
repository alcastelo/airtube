package com.thinktube.net.nio.blocking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;

public class UDPPeriodicSenderThread extends Thread {
	private volatile boolean running = true;
	ByteBuffer[] buffers;
	UDPSendCallback sendCb;
	private long delay;
	DatagramChannel channel;
	InetSocketAddress target;

	public UDPPeriodicSenderThread(InetSocketAddress target, long period) {
		setDaemon(true);
		setName("UDPPeriodicSenderThread");
		try {
			channel = DatagramChannel.open();
			channel.socket().setReuseAddress(true);
			channel.socket().setBroadcast(true);
			//channel.socket().bind(target);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.target = target;
		this.delay = period;
	}

	@Override
	public void run() {
		//Log.d(TAG, "run");
		try {
			channel.connect(target);
			System.out.println("OOO connected OOO to " + target);
		} catch (IOException e2) {
			e2.printStackTrace();
			return;
		}

		while (running) {
			if (sendCb != null) {
				try {

					buffers = sendCb.getPacketData();
					channel.write(buffers);

				} catch (ClosedChannelException e) {
					if (running)
						e.printStackTrace();
					// socket was closed in stopRunning
					break;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}

			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				; //Log.d(TAG, "who woke me up?");
			}
		}
		//Log.d(TAG, "*** end run");
	}

	public void setCallback(UDPSendCallback cb) {
		sendCb = cb;
	}

	public void stopRunning() {
		//Log.d(TAG, "*** stopping");
		running = false;
		try {
			channel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
