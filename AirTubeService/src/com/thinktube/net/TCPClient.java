package com.thinktube.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import android.util.Log;

public class TCPClient {
	private static final String TAG = "TCPClient";
	protected Socket socket;
	protected BufferedReader in;
	protected PrintWriter out;

	public TCPClient(InetAddress ip, int port) throws IOException {
		socket = new Socket(ip, port);
		openIO();
	}

	public TCPClient(String ip, int port) throws IOException {
		socket = new Socket(ip, port);
		openIO();
	}

	private void openIO() throws IOException {
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(socket.getOutputStream(), true); // autoflush
	}

	public void send(String s) {
		out.println(s);
	}

	public String expectReply(String... answers) throws IOException {
		String line = readReply();
		for (String s : answers) {
			if (line.startsWith(s))
				return line;
		}
		Log.d(TAG, "unexpected reply: " + line);
		return line;
	}

	public String readReply() throws IOException {
		String line;
		if ((line = in.readLine()) != null) {
			Log.d(TAG, "received: " + line);
			return line;
		} else
			throw new IOException("End of Stream");
	}

	public void close() {
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
