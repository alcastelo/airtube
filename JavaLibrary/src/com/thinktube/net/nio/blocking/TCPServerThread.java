package com.thinktube.net.nio.blocking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class TCPServerThread extends Thread {
	private volatile boolean running = true;
	ServerSocketChannel serverChannel;
	List<TCPServerConnection> clients = new ArrayList<TCPServerConnection>();
	TCPServerConnectionFactory factory;

	public TCPServerThread(int port, TCPServerConnectionFactory f) {
		setDaemon(true);
		setName("TCPServerThread");
		this.factory = f;
		try {
			serverChannel = ServerSocketChannel.open();
			serverChannel.socket().bind(new InetSocketAddress(port));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		///Log.d(TAG, "TCP server running...");

		while (running) {
			SocketChannel clientChannel;
			try {
				clientChannel = serverChannel.accept();
				if (clientChannel != null) {
					TCPServerConnection client = factory.create(clientChannel, this);
					client.start();
					clients.add(client);
				}
			} catch (IOException e) {
				if (running)
					e.printStackTrace();
				// else the socket was closed by stopRunning and this is OK
			}
		}

		//Log.d(TAG, "end run");
	}

	public void stopRunning() {
		running = false;
		//Log.d(TAG, "stopping");

		try {
			serverChannel.close();
		} catch (IOException e) {
			// ignore
		}

		for (TCPServerConnection c : clients) {
			//Log.d(TAG, "stopping server connection");
			c.stopRunning();
		}
	}

	public void removeClient(TCPServerConnection client) {
		clients.remove(client);
	}
}
