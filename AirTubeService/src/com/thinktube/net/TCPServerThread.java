package com.thinktube.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class TCPServerThread extends Thread {
	private static final String TAG = "TCPServer";
	private volatile boolean running = true;
	TCPReceiveCallback recvCallback = null;
	ServerSocket serverSocket;
	List<TCPServerConnection> clients = new ArrayList<TCPServerConnection>();

	public TCPServerThread(int port) {
		setDaemon(true);

		try {
			serverSocket = new ServerSocket(port);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {

		Log.d(TAG, "TCP server running...");

		while (running) {
			Socket clientSock;
			try {
				clientSock = serverSocket.accept();

				if (clientSock != null) {
					TCPServerConnection client = new TCPServerConnection(
							clientSock, recvCallback);
					client.start();
					clients.add(client);
				}
			} catch (IOException e) {
				if (running)
					e.printStackTrace();
				// else the socket was closed by stopRunning and this is OK
			}
		}

		Log.d(TAG, "end run");
	}

	public void setReceiveCallback(TCPReceiveCallback cb) {
		recvCallback = cb;
	}

	public void stopRunning() {
		running = false;
		Log.d(TAG, "stopping");

		try {
			serverSocket.close();
		} catch (IOException e) {
			// TODO ignore
			e.printStackTrace();
		}

		for (TCPServerConnection c : clients) {
			Log.d(TAG, "stopping server connection");
			c.stopRunning();
		}
	}
}
