package com.thinktube.airtube;

import java.io.IOException;
import java.net.InetAddress;

import com.thinktube.net.NetworkInterfaces;
import com.thinktube.net.nio.PktConnectionCallback;
import com.thinktube.net.nio.PktConnection;

public interface DataTransmitI {

	void start(PktConnectionCallback receiveCb, NetworkInterfaces nifs) throws IOException;

	void stop();

	PktConnection createConnection(InetAddress ip, TrafficClass tos, PktConnectionCallback receiveCb) throws IOException;
}
