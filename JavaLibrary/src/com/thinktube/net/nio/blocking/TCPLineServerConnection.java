package com.thinktube.net.nio.blocking;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;

/* this is a simple line by line implementation */

class TCPLineServerConnection extends TCPServerConnection {
	//private static final String TAG = "TCPLineServerConnection";
	private BufferedReader in;
	private PrintWriter out;
	TCPLineReceiveCallback recvCb;
	String line;

	public TCPLineServerConnection(SocketChannel c, TCPLineReceiveCallback cb, TCPServerThread serv) {
		super(c, serv);
		//Log.d(TAG, "*** created");
		recvCb = cb;
		in = new BufferedReader(new InputStreamReader(Channels.newInputStream(c)));
		out = new PrintWriter(Channels.newOutputStream(c));
	}

	public synchronized boolean handle() throws IOException {
		line = in.readLine();
		if (line == null) { // client closed socket
			return false;
		}
		if (recvCb != null) {
			recvCb.handleLine(line, in, out, channel);
		}
		return true;
	}

	public void close() {
		//Log.d(TAG, "*** closing");
		try {
			in.close();
		} catch (IOException e) {
			// ignore
		}
		out.close();
	}
}
