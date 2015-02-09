package com.thinktube.net;

import java.nio.ByteBuffer;

public interface Packet {

	void fromByteBuffer(ByteBuffer buf);

	ByteBuffer[] toByteBuffers();
}
