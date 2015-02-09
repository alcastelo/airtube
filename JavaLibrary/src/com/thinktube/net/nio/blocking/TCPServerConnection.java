package com.thinktube.net.nio.blocking;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public abstract class TCPServerConnection extends Thread {
	protected SocketChannel channel;
	protected TCPServerThread server;
	protected volatile boolean running = true;

	public TCPServerConnection(SocketChannel c, TCPServerThread serv) {
		setDaemon(true);
		setName("TCP Server connection");
		channel = c;
		server = serv;
	}

	@Override
	public void run() {
		try {
			while (running) {
				if (!handle()) {
					break;
				}
			}
		} catch (IOException e) {
			if (running) {
				e.printStackTrace();
			}
			// else the socket was closed by stopRunning and this is OK
		}
		close();
		try {
			channel.close();
		} catch (IOException e) {
			// ignore
		}

		server.removeClient(this);
		//Log.d(TAG, "*** end run");
	}

	protected abstract boolean handle() throws IOException;
	protected abstract void close();

	public void stopRunning() {
		running = false;
		try {
			channel.close();
		} catch (IOException e) {
			// ignore
		}
	}
}
