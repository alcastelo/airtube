package com.thinktube.audio;

import java.util.logging.Logger;

import com.thinktube.util.CircularPriorityQueueBuffer;

/**
 * This is a static fixed-size jitter buffer which uses
 * CircularPriorityQueueBuffer.
 *
 * Note that instead of "vitualClock" calculations, we rely on getPlayBuffer()
 * being called in time (every sample period) and we return frames based on the
 * sequence number there. If a frame was lost we have to output "nothing" to
 * keep the buffer filled (and delay constant) and we rely on the audio player
 * or decoder to substitute this with silence.
 *
 * @author br1
 */
public class JitterBuffer3 implements JitterBuffer {
	private final static Logger LOG = Logger.getLogger(JitterBuffer3.class.getSimpleName());

	private final CircularPriorityQueueBuffer<BufferEntry> queue;
	private Statistics stats = new Statistics();
	private boolean fillBuffers = true;
	private int lastPlayedSeq = -1;

	// just used for delay calculation and updating statistics
	private long time;
	private long lastTime = -1;
	private long delay;

	public JitterBuffer3(AudioConfig audioConfig, int maxDelay) {
		int maxBuffers = audioConfig.getFrameCountForMs(maxDelay);
		LOG.fine("Initialzing for " + maxDelay + "ms delay with "
				+ maxBuffers + " buffers (" + audioConfig.getFramePeriod()
				+ "ms each)");

		/* pre-allocate buffers */
		BufferEntry[] elems = new BufferEntry[maxBuffers];
		for (int i = 0; i < maxBuffers; i++) {
			elems[i] = new BufferEntry();
		}

		queue = new CircularPriorityQueueBuffer<BufferEntry>(elems);
	}

	public synchronized void receive(byte[] buffer, int offset, int len, int seqNo, byte flags) {
		stats.receivedPackets++;

		/* simulate packet loss of 30%
		if (stats.receivedPackets % 3 == 0) {
			Log.d(TAG, "drop " + seqNo);
			return;
		}
		*/

		getDelay();
		LOG.fine("seqNo " + seqNo + " received after " + delay + "ms");

		if (seqNo <= lastPlayedSeq) { // too late to re-order...
			LOG.fine("discarding late packet " + seqNo);
			stats.latePackets++;
			return;
		}

		BufferEntry be = queue.getNextFree();

		// if buffer was removed from head we have to update lastPlayedSeq
		if (lastPlayedSeq != -1 && be.seqNo > lastPlayedSeq) {
			lastPlayedSeq = be.seqNo;
			LOG.fine("playing too slow?");
		}

		be.seqNo = seqNo;
		be.data = buffer;
		be.offset = offset;
		be.len = len;

		boolean res = queue.put(be);

		if (!res) {
			LOG.fine("queue dropped duplicate frame");
			stats.duplicatePackets++;
		}

		// buffer is full again
		if (fillBuffers && (queue.length() == queue.maxLength())) {
			LOG.fine("end fill phase");
			fillBuffers = false;
		}
	}

	private void getDelay() {
		time = System.currentTimeMillis();

		if (lastTime == -1) { // first time
			delay = 0;
		} else {
			delay = time - lastTime;
			stats.updateDelay(delay);
		}
		lastTime = time;
	}

	public synchronized void reset() {
		LOG.fine("reset");
		LOG.info(stats.toString());

		queue.clear();
		stats.clear();
		lastTime = -1;
		lastPlayedSeq = -1;
		fillBuffers = true;
	}

	/** Return the next buffer for playing or null if none is available (yet)
	 * In the special case of FEC the next packet is returned with the fec flag set */
	@Override
	public synchronized BufferEntry getPlayBuffer() {
		if (fillBuffers)
			return null;

		BufferEntry be = queue.peek();

		if (be == null) {
			// queue empty
			stats.bufferUnderruns++;
			fillBuffers = true;
			//LOG.fine("start fill phase");
		} else if (lastPlayedSeq == -1 || be.seqNo == lastPlayedSeq + 1) {
			// initial packet or correct sequence
			queue.poll();
			be.fec = false;
			lastPlayedSeq = be.seqNo;
		} else if (be.seqNo == lastPlayedSeq + 2) {
			// we lost the current frame, but can do FEC with the next one
			// don't remove this buffer from the queue as we will need it next
			//LOG.fine("FEC with " + be.seqNo);
			be.fec = true;
			lastPlayedSeq++;
		} else if (be.seqNo > lastPlayedSeq + 2) {
			// we lost too many frames
			//LOG.fine("lost seqNo " + (lastPlayedSeq + 1));
			stats.lostPackets++;
			lastPlayedSeq++;
			be = null;
		} else if (be.seqNo < lastPlayedSeq + 1) {
			// just discard it and hope for the next packet
			LOG.warning("should not happen");
			queue.poll();
			be = null;
		}
		stats.playedPackets++;
		return be;
	}

	@Override
	public synchronized void returnPlayBuffer(BufferEntry be) {
		queue.returnFree(be);
	}

	@Override
	public Statistics getStatistics() {
		stats.buffersInUse = queue.length();
		return stats;
	}
}
