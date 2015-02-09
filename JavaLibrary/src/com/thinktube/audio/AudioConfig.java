package com.thinktube.audio;

public final class AudioConfig {
	/** Hz = times per second */
	public final int sampleRate;

	/** size of one frame in samples, not bytes */
	public int frameSize;

	/* keep the following as constants because we don't change them ATM */

	/** for now only mono */
	public final int CHANNELS = 1;

	/** size of one sample in bytes = 16 bit in bytes (ENCODING_PCM_16BIT) */
	public final int SAMPLE_SIZE = 2;

	public AudioConfig(int sampleRate, int frameSize) {
		this.sampleRate = sampleRate;
		this.frameSize = frameSize;
	}

	public AudioConfig(int sampleRate, int frameSize, boolean frameSizeInMs) {
		this.sampleRate = sampleRate;
		if (frameSizeInMs) {
			this.frameSize = getSamplesForMs(frameSize);
		} else {
			this.frameSize = frameSize;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + CHANNELS;
		result = prime * result + SAMPLE_SIZE;
		result = prime * result + frameSize;
		result = prime * result + sampleRate;
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
		AudioConfig other = (AudioConfig) obj;
		if (CHANNELS != other.CHANNELS)
			return false;
		if (SAMPLE_SIZE != other.SAMPLE_SIZE)
			return false;
		if (frameSize != other.frameSize)
			return false;
		if (sampleRate != other.sampleRate)
			return false;
		return true;
	}

	/** size of one frame in bytes */
	public int getFrameBytes() {
		return frameSize * SAMPLE_SIZE;
	}

	/** time of one frame in ms */
	public int getFramePeriod() {
		return frameSize * 1000 / sampleRate;
	}

	/** number of samples per ms */
	public int getSamplesPerMs() {
		return sampleRate / 1000;
	}

	/** number of samples for delay in ms */
	public int getSamplesForMs(int delay) {
		return getSamplesPerMs() * delay;
	}

	/** number of bytes for delay in ms */
	public int getBytesForMs(int delay) {
		return getSamplesForMs(delay) * SAMPLE_SIZE;
	}

	/** rounded number of frames for delay in ms */
	public int getFrameCountForMs(int delay) {
		return delay / getFramePeriod();
	}

	@Override
	public String toString() {
		return "AudioConfig [sampleRate=" + sampleRate + ", frameSize=" + frameSize + " (" + getFramePeriod() + "ms)]";
	}
}
