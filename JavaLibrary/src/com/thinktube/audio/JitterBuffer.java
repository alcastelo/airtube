package com.thinktube.audio;

public interface JitterBuffer {
	public class Statistics {
		public long receivedPackets = 0;
		public long latePackets = 0;
		public long duplicatePackets = 0;
		public long lostPackets = 0;
		public long playedPackets = 0;
		public int buffersInUse = 0;
		public long bufferUnderruns = 0;
		public long sumDelay = 0;
		public long maxDelay = 0;
		public long minDelay = 10000;

		@Override
		public String toString() {
			return "Statistics ["
					+ "receivedPackets=" + receivedPackets
					+ ", latePackets=" + latePackets
					+ ", duplicatePackets=" + duplicatePackets
					+ ", lostPackets=" + lostPackets
					+ ", playedPackets=" + playedPackets
					+ ", buffersInUse=" + buffersInUse
					+ ", bufferUnderruns=" + bufferUnderruns
					+ ", averageDelay="	+ (receivedPackets > 0 ? sumDelay/receivedPackets : 0)
					+ ", maxDelay=" + maxDelay
					+ ", minDelay=" + minDelay + "]";
		}

		public void clear() {
			receivedPackets = 0;
			latePackets = 0;
			duplicatePackets = 0;
			lostPackets = 0;
			playedPackets = 0;
			buffersInUse = 0;
			bufferUnderruns = 0;
			sumDelay = 0;
			maxDelay = 0;
			minDelay = 10000;
		}

		public void updateDelay(long delay) {
			sumDelay += delay;
			if (delay > maxDelay)
				maxDelay = delay;
			if (delay < minDelay)
				minDelay = delay;
		}
	};

	class BufferEntry implements Comparable<BufferEntry> {
		public int seqNo;
		public byte[] data;
		public int offset;
		public int len;
		public boolean fec;

		BufferEntry() {
			seqNo = -1;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + seqNo;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			BufferEntry other = (BufferEntry) obj;
			if (seqNo != other.seqNo)
				return false;
			return true;
		}

		/** we need to implement this for the PriorityQueue order */
		@Override
		public int compareTo(BufferEntry other) {
			// a negative integer if this instance is less than another;
			// a positive integer if this instance is greater than another;
			// 0 if this instance has the same order as another.
			if (this.seqNo == other.seqNo)
				return 0;
			else if (this.seqNo < other.seqNo)
				return -1;
			return 1;
		}
	};

	void receive(byte[] buffer, int offset, int len, int seqNo, byte flags);
	BufferEntry getPlayBuffer();
	void returnPlayBuffer(BufferEntry be);
	Statistics getStatistics();
	void reset();
}
