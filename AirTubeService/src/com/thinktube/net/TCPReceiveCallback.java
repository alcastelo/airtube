package com.thinktube.net;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

public interface TCPReceiveCallback {
	public void handleLine(String s, BufferedReader in, PrintWriter out,
			Socket socket);
}
