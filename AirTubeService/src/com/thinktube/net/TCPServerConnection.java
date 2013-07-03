package com.thinktube.net;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import android.util.Log;

/* this is a simple line by line implementation */

class TCPServerConnection extends Thread {
	private static final String TAG = "TCPServerConnection";
	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;
	TCPReceiveCallback recvCb;
	private volatile boolean running = true;

	public TCPServerConnection(Socket c, TCPReceiveCallback cb) {
		setDaemon(true);

		socket = c;
		recvCb = cb;
	}

	@Override
	public void run() {
		try {
			in = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream());

			String line;

			while (running) {
				line = in.readLine();
				if (line == null) { // client closed socket
					break;
				}

				if (recvCb != null) {
					recvCb.handleLine(line, in, out, socket);
				}
			}
		} catch (IOException e) {
			if (running)
				e.printStackTrace();
			// else the socket was closed by stopRunning and this is OK
		}

		Log.d(TAG, "*** out of loop");
		try {
			in.close();
		} catch (IOException e) {
			// ignore
		}
		out.close();
		try {
			socket.close();
		} catch (IOException e) {
			// ignore
		}
		Log.d(TAG, "*** end run");
	}

	public void stopRunning() {
		running = false;

		try {
			socket.close();
		} catch (IOException e) {
			// ignore
		}

		// TODO: remove from TCPServer clients list somehow
	}
}
