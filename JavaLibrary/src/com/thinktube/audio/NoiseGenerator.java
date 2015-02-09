package com.thinktube.audio;

/**
 * Generates "Comfort" Noise
 * 
 * @author br1
 */
public class NoiseGenerator {
	private static final short NOISE_VOLUME = 16; // 16 is very low, we could use up to Short.MAX_VALUE
	private short[] buffer;

	public NoiseGenerator(int bufferSize) {
		buffer = new short[bufferSize];
		for (int i = 0; i < buffer.length; i++) {
			buffer[i] = (short) (Math.random() * NOISE_VOLUME);
		}
	}

	public short[] makeSound() {
		final int len = buffer.length;
		for (int i = 0; i < 40; i++) {
			int j = (int) (Math.random() * len);
			int k = (int) (Math.random() * len);
			short tmp = buffer[j];
			buffer[j] = buffer[k];
			buffer[k] = tmp;
		}
		return buffer;
	}

	public int getBufferLength() {
		return buffer.length;
	}
}
