package com.thinktube.net.nio.blocking;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface UDPSendCallback {
	ByteBuffer[] getPacketData() throws IOException;
}
