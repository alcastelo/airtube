package com.thinktube.codec;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

import com.thinktube.util.ArrayUtil;
import com.thinktube.util.NumberConversion;

public class VideoProtocol {
	private final static Logger LOG = Logger.getLogger(VideoProtocol.class.getSimpleName());
	private static final int PKT_SIZE = 1447;
	private static final int PKT_HEADER_SIZE = 7;
	private static final int PKT_DATA_SIZE = PKT_SIZE - PKT_HEADER_SIZE;

	public static class OutCallback implements CodecOutCallbackI {
		private int seqNo = 0;
		// store outside of function to avoid GC:
		private byte[] chunk = new byte[PKT_SIZE];
		private int get_len;
		private int remaining;
		private short fragNo;
		private short numberOfFragments;
		private byte flags;
		private final ProtocolSenderI sender;

		public OutCallback(ProtocolSenderI sender) {
			super();
			this.sender = sender;
		}

		@Override
		public void handleFrame(ByteBuffer buf, int size, int inflags) {
			remaining = size;
			fragNo = 1;
			numberOfFragments = (short)(size/PKT_DATA_SIZE + 1);

			if ((inflags & Codec.FLAG_SYNC_FRAME) != 0) {
				LOG.fine("+++++ SYNC +++++");
				flags = Codec.FLAG_SYNC_FRAME;
			}
			else if ((inflags & Codec.FLAG_CODEC_CONFIG) != 0) {
				LOG.fine("+++++ CONFIG +++++ " + size);
				flags = Codec.FLAG_CODEC_CONFIG;
			}
			else
				flags = (byte) 0x00;

			//Log.d(TAG, "sending pkt seq " + seqNo + " fragments " + numberOfFragments + " data len " + info.size);

			do {
				get_len = (remaining < PKT_DATA_SIZE ? remaining : PKT_DATA_SIZE);

				//Log.d(TAG, "sending seq " + seqNo + " frag " + fragNo + " of " + numberOfFragments + " len " + get_len);

				chunk[0] = flags;							// flags
				NumberConversion.intTo(seqNo, chunk, 1);	// sequence number
				chunk[5] = (byte)(fragNo++ & 0xFF);			// fragment number
				chunk[6] = (byte)(numberOfFragments & 0xFF);// number of fragments
				buf.get(chunk, PKT_HEADER_SIZE, get_len);	// data

				sender.sendPacket(chunk, get_len + PKT_HEADER_SIZE);

				remaining -= get_len;
			}
			while (remaining > 0);

			seqNo++;
		}

		@Override
		public void handleFrame(byte[] buf, int size) {
			// not used by video codecs so far
		}

		@Override
		public void handleFrame(short[] buf, int size) {
			// not used by video codecs so far
		}

		@Override
		public void stop() {
			// ignore
		}

		@Override
		public int getNecessaryHeadroom() {
			return 0; // not used so far but could be!
		}
	}

	public static class RecvCallback {
		private static final int MAX_FRAME_SIZE = 200000;
		private static final int MAX_FRAGMENTS = 255;

		private final ProtocolReceiverI receiver;
		// store outside to avoid GC:
		private byte[] buffer;
		private byte[] frame = new byte[MAX_FRAME_SIZE];
		private boolean[]fragment_set = new boolean[MAX_FRAGMENTS];
		private int flags, lastFlags;
		private boolean needDecode;
		private int lastSeqNo = 0, seqNo;
		private short fragNo, numberOfFragments, lastNumberOfFragments;
		private int dataLen;

		public RecvCallback(ProtocolReceiverI recv) {
			this.receiver = recv;
		}

		public void handlePacket(byte[] data) {
			buffer = data;
			dataLen = buffer.length - PKT_HEADER_SIZE;
			seqNo = NumberConversion.intFrom(buffer, 1);
			fragNo = buffer[5];
			numberOfFragments = buffer[6];
			flags = 0;

			//Log.d("FRR", "receive seq " + seqNo + " frag " + fragNo + "/" + numberOfFragments + " len " + buffer.length + " data " + dataLen);

			if ((buffer[0] & Codec.FLAG_CODEC_CONFIG) != 0) {
				LOG.fine("+++++ CONFIG +++++ " + dataLen);

				// assuming config packets are not fragmented!
				// here we completely re-initialize the codec.
				// we could also try to pass the flag BUFFER_FLAG_CODEC_CONFIG

				//hwDec.stop();
				//hwDec.init(ByteBuffer.wrap(buffer, PKT_HEADER_SIZE, dataLen));
				//hwDec.start();

				lastSeqNo = 0;
				needDecode = false;
				flags = 0; lastFlags = 0;
				return;

			} else if ((buffer[0] & Codec.FLAG_SYNC_FRAME) != 0) {
				//LOG.info("+++++ SYNC +++++ " + fragNo);
				flags = Codec.FLAG_SYNC_FRAME;
			}

			/* handle according to sequence number */

			if (seqNo < lastSeqNo) {
				LOG.fine("***** discarding late packet");
				return;
			}
			else if (seqNo == lastSeqNo) {
				if (fragment_set[fragNo]) {
					LOG.fine("**** discarding duplicate fragment");
					return;
				} else {
					copyData(buffer, dataLen, fragNo);
				}
			}
			else if (seqNo > lastSeqNo) { // new packet
				//decode old data if present (maybe last fragment was lost)
				 if (needDecode) {
					LOG.fine("*** decoding seq " + lastSeqNo + " because of new seqNo " + seqNo + ", data incomplete");
					decode(PKT_DATA_SIZE, lastNumberOfFragments, lastFlags);
				}
				//save new data
				copyData(buffer, dataLen, fragNo);
			}

			if (fragNo == numberOfFragments) {
				//Log.d(TAG, "*** last fragment, decoding seq " + seqNo);
				decode(dataLen, numberOfFragments, flags);
			}

			lastSeqNo = seqNo;
			lastNumberOfFragments = numberOfFragments;
			lastFlags = flags;
		}

		private void copyData(byte[] pkt, int pktLen, int fragNo) {
			// copies data into the right place depending on fragment number
			System.arraycopy(pkt, PKT_HEADER_SIZE, frame, (fragNo-1)*PKT_DATA_SIZE, pktLen);
			fragment_set[fragNo] = true;
			needDecode = true;
		}

		private void decode(int lastFragLen, int numberOfFragments, int flags) {
			// complain about missing fragments and re-set flags
			int setCount = 0;
			for (int i = 1; i <= numberOfFragments; i++) {
				if (!fragment_set[i]) {
					//Log.d(TAG, "** fragment " + i + " of " + numberOfFragments + " missing!");
				}
				else {
					setCount++;
					fragment_set[i] = false;
					//Log.d(TAG, "** fragment " + i + " of " + numberOfFragments + " OK");
				}
			}
			int frameLen = (numberOfFragments-1)*PKT_DATA_SIZE + lastFragLen;

			if (receiver.isReady() && setCount >= numberOfFragments/2) {
				if (setCount < numberOfFragments)
					LOG.fine("decoding incomplete frame: " + setCount + " of " + numberOfFragments + " fragments received");

				//Log.d(TAG, "decoding frame len " + frameLen + " from " + numberOfFragments + " fragments, flags " + flags);
				receiver.receiveFrame(frame, 0, frameLen, flags);
			} else
				LOG.fine("***** not decoding");

			ArrayUtil.bytefillZero(frame);
			flags = 0; lastFlags = 0;
			needDecode = false;
		}

		public void reset() {
			LOG.fine("reset");
			lastSeqNo = 0;
			needDecode = false;
			lastFlags = 0;
		}
	}
}
