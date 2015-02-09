package com.thinktube.net.nio.blocking;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.nio.channels.SocketChannel;

public interface TCPLineReceiveCallback {
	void handleLine(String s, BufferedReader in, PrintWriter out, SocketChannel channel);
}
