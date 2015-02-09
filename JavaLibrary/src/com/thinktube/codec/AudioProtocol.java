package com.thinktube.codec;

import java.nio.ByteBuffer;

import com.thinktube.audio.JitterBuffer;
import com.thinktube.util.NumberConversion;

/* The Audio Protocol is very simple:
 *
 * 4 byte	sequence number
 * rest		data
 */

public class AudioProtocol {
	/* PKT_SIZE calculation:
	 *
	 * IP header		20 byte
	 * UDP header		 8 byte
	 * AirTube header	21 byte (1B version, 2*10B src/dst AirTubeID)
	 * ServiceData len	 4 byte (int 32 bit, could be made 16bit)
	 * ------------------------
	 * ALL HEADERS		53 byte
	 * Maximum ServiceData size = 1500 - 53 = 1447
	 */
	private static final int PKT_SIZE = 1447;
	private static final int PKT_HEADER_SIZE = 4;
	//private static final int PKT_DATA_SIZE = PKT_SIZE - PKT_HEADER_SIZE;

	public static class OutCallback implements CodecOutCallbackI {
		//private static final String TAG = "AudioProtocolSend";
		private byte[] chunk = new byte[PKT_SIZE];	// store outside of function to avoid GC:
		private int seqNo = 0;
		private ProtocolSenderI sender;

		public OutCallback(ProtocolSenderI sender) {
			super();
			this.sender = sender;
		}

		@Override
		public void handleFrame(ByteBuffer buf, int size, int unused_flags) {
			//Log.d(TAG, "sending pkt seq " + seqNo + " data len " + info.size);

			NumberConversion.intTo(seqNo, chunk, 0);	// sequence number, 4 bytes
			buf.get(chunk, PKT_HEADER_SIZE, size);	// data

			sender.sendPacket(chunk, size + PKT_HEADER_SIZE);
			seqNo++;
		}

		/**
		 * This assumes there is sufficient space as specified by
		 * getNecessaryHeadroom() for the packet header at the beginning!!!
		 */
		@Override
		public void handleFrame(byte[] buf, int size) {
			NumberConversion.intTo(seqNo, buf, 0);	// sequence number, 4 bytes

			sender.sendPacket(buf, size + PKT_HEADER_SIZE);
			seqNo++;
		}

		@Override
		public void handleFrame(short[] buf, int size) {
			// not used by audio codecs so far, as encoder output is usually bytes
		}

		@Override
		public void stop() {
			// ignore
		}

		@Override
		public int getNecessaryHeadroom() {
			return PKT_HEADER_SIZE;
		}
	}

	public static class Receiver {
		//private static final String TAG = "AudioProtocolRecv";
		private final JitterBuffer jb;
		private int dataLen;
		private int seqNo;
		private byte flags;

		public Receiver(JitterBuffer jb) {
			this.jb = jb;
		}

		public void handlePacket(byte[] buffer) {
			dataLen = buffer.length - PKT_HEADER_SIZE;
			seqNo = NumberConversion.intFrom(buffer, 0);

			//Log.d("FRR", "receive seq " + seqNo + " len " + buffer.length + " data " + dataLen);
			jb.receive(buffer, PKT_HEADER_SIZE, dataLen, seqNo, flags);
		}
	}
}
