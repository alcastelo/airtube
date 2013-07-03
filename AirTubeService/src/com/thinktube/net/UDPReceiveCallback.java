package com.thinktube.net;

import java.net.DatagramPacket;

public interface UDPReceiveCallback {
	public void handlePacket(DatagramPacket p);
}
