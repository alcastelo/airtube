package com.thinktube.audio;

/**
 * Generates Sine Wave tones
 * 
 * @author br1
 */
public class ToneGenerator {
	private final int sampleRate;
	private short[] buffer;
	private float angle = 0;

	public ToneGenerator(int bufferSize, int sampleRate) {
		this.buffer = new short[bufferSize];
		this.sampleRate = sampleRate;
	}

	public short[] makeSound(float freq) {
		for (int i = 0; i < buffer.length; i++) {
			float angular_frequency = (float) (2 * Math.PI) * freq / sampleRate;
			buffer[i] = (short) (Short.MAX_VALUE * ((float) Math.sin(angle)));
			angle += angular_frequency;
		}
		return buffer;
	}
	
	public int getBufferLength() {
		return buffer.length;
	}
}
