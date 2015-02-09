package com.thinktube.jni;

public class OpusDecoderNative {

	public native int create(int sampleRate, int channels);

	public native int decode(byte input[], int inOffset, int inSize,
			short output[], int outOffset, int outSize, int fec);

	public native void destroy();

	/**
	 * Gets the encoder's configured bandpass or the decoder's last bandpass.
	 * 
	 * @return Returns one of the following values:
	 * 
	 *         OPUS_AUTO (default) OPUS_BANDWIDTH_NARROWBAND 4 kHz passband
	 *         OPUS_BANDWIDTH_MEDIUMBAND 6 kHz passband OPUS_BANDWIDTH_WIDEBAND
	 *         8 kHz passband OPUS_BANDWIDTH_SUPERWIDEBAND 12 kHz passband
	 *         OPUS_BANDWIDTH_FULLBAND 20 kHz passband
	 */
	public native int getBandwidth();

	/**
	 * Gets the sampling rate the encoder or decoder was initialized with.
	 * 
	 * This simply returns the Fs value passed to opus_encoder_init() or
	 * opus_decoder_init().
	 * 
	 * @return
	 */
	public native int getSampleRate();

	/**
	 * Gets the pitch of the last decoded frame, if available.
	 * 
	 * This can be used for any post-processing algorithm requiring the use of
	 * pitch, e.g. time stretching/shortening. If the last frame was not voiced,
	 * or if the pitch was not coded in the frame, then zero is returned.
	 * 
	 * This CTL is only implemented for decoder instances.
	 * 
	 * @return
	 */
	public native int getPitch();

	/**
	 * Gets the decoder's configured gain adjustment.
	 * 
	 * @return Amount to scale PCM signal by in Q8 dB units.
	 */
	public native int getGain();

	/**
	 * Configures decoder gain adjustment.
	 * 
	 * Scales the decoded output by a factor specified in Q8 dB units. This has
	 * a maximum range of -32768 to 32767 inclusive, and returns OPUS_BAD_ARG
	 * otherwise. The default is zero indicating no adjustment. This setting
	 * survives decoder reset.
	 * 
	 * gain = pow(10, x/(20.0*256))
	 * 
	 * @param val
	 *            Amount to scale PCM signal by in Q8 dB units.
	 * @return
	 */
	public native int setGain(int val);

	/**
	 * Gets the duration (in samples) of the last packet successfully decoded or
	 * concealed.
	 * 
	 * @return Number of samples (at current sampling rate).
	 */
	public native int getLastPacketDuration();

	/**
	 * Resets the codec state to be equivalent to a freshly initialized state.
	 * 
	 * This should be called when switching streams in order to prevent the back
	 * to back decoding from giving different results from one at a time
	 * decoding.
	 * 
	 * @return
	 */
	public native int resetState();
	
	static {
		System.loadLibrary("opus");
	}
}
