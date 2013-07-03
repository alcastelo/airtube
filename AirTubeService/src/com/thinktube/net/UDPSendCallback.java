package com.thinktube.net;

import java.io.IOException;

public interface UDPSendCallback {
	public byte[] getPacketData() throws IOException;
}
