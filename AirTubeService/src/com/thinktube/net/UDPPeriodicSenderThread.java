package com.thinktube.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import android.util.Log;

public class UDPPeriodicSenderThread extends Thread {
	private static final String TAG = "SASender";
	private volatile boolean running = true;
	byte[] buffer;
	DatagramPacket packet = null;
	UDPSendCallback sendCb;
	private long delay;
	DatagramSocket socket;
	InetAddress target;
	int port;

	public UDPPeriodicSenderThread(DatagramSocket s, InetAddress target,
			int port, long period) {
		setDaemon(true);

		this.socket = s;
		this.target = target;
		this.port = port;
		this.delay = period;
	}

	@Override
	public void run() {
		Log.d(TAG, "run");

		while (running) {
			if (sendCb != null) {
				try {

					buffer = sendCb.getPacketData();
					packet = new DatagramPacket(buffer, buffer.length, target,
							port);
					socket.send(packet);

				} catch (IOException e) {
					if (running)
						e.printStackTrace();
					else
						// socket was closed in stopRunning
						break;
				}
			}

			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				Log.d(TAG, "who woke me up?");
			}
		}
		Log.d(TAG, "*** end run");
	}

	public void setCallback(UDPSendCallback cb) {
		sendCb = cb;
	}

	public void stopRunning() {
		Log.d(TAG, "*** stopping");
		running = false;
		socket.close();
	}
}
