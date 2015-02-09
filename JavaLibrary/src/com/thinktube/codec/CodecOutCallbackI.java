package com.thinktube.codec;

import java.nio.ByteBuffer;

public interface CodecOutCallbackI {
	/* Android specific callbacks extend this interface */

	/* depending on the codec one of these functions will be called */
	void handleFrame(byte[] buf, int size);
	void handleFrame(short[] buf, int size);
	void handleFrame(ByteBuffer buf, int size, int flags);

	void stop();

	/** offset which has to be left empty at the beginning of the frame */
	int getNecessaryHeadroom();
}
