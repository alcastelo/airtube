package com.thinktube.jni;

public class OpusEncoderNative {
	/*
	 * These constants need to be synchronized with
	 * jni/opus/opus-1.1/include/opus_defines.h
	 */

	/**
	 * Auto/default setting
	 */
	public static final int OPUS_AUTO = -1000;

	/**
	 * Maximum bitrate.
	 */
	public static final int OPUS_BITRATE_MAX = -1;

	/**
	 * OPUS_APPLICATION_VOIP gives best quality at a given bitrate for voice
	 * signals. It enhances the input signal by high-pass filtering and
	 * emphasizing formants and harmonics. Optionally it includes in-band
	 * forward error correction to protect against packet loss. Use this mode
	 * for typical VoIP applications. Because of the enhancement, even at high
	 * bitrates the output may sound different from the input.
	 * 
	 */
	public static final int OPUS_APPLICATION_VOIP = 2048;

	/**
	 * OPUS_APPLICATION_AUDIO gives best quality at a given bitrate for most
	 * non-voice signals like music. Use this mode for music and mixed
	 * (music/voice) content, broadcast, and applications requiring less than 15
	 * ms of coding delay.
	 */
	public static final int OPUS_APPLICATION_AUDIO = 2049;

	/**
	 * OPUS_APPLICATION_RESTRICTED_LOWDELAY configures low-delay mode that
	 * disables the speech-optimized mode in exchange for slightly reduced
	 * delay. This mode can only be set on an newly initialized or freshly reset
	 * encoder because it changes the codec delay.
	 * 
	 * This is useful when the caller knows that the speech-optimized modes will
	 * not be needed (use with caution).
	 */
	public static final int OPUS_APPLICATION_RESTRICTED_LOWDELAY = 2051;

	/**
	 * Signal being encoded is voice.
	 */
	public static final int OPUS_SIGNAL_VOICE = 3001;

	/**
	 * Signal being encoded is music.
	 */
	public static final int OPUS_SIGNAL_MUSIC = 3002;

	/** 4 kHz bandpass */
	public static final int OPUS_BANDWIDTH_NARROWBAND = 1101;
	/** 6 kHz bandpass */
	public static final int OPUS_BANDWIDTH_MEDIUMBAND = 1102;
	/** 8 kHz bandpass */
	public static final int OPUS_BANDWIDTH_WIDEBAND = 1103;
	/** 12 kHz bandpass */
	public static final int OPUS_BANDWIDTH_SUPERWIDEBAND = 1104;
	/** 20 kHz bandpass */
	public static final int OPUS_BANDWIDTH_FULLBAND = 1105;

	/** Select frame size from the argument (default) */
	public static final int OPUS_FRAMESIZE_ARG = 5000;
	/** Use 2.5 ms frames */
	public static final int OPUS_FRAMESIZE_2_5_MS = 5001;
	/** Use 5 ms frames */
	public static final int OPUS_FRAMESIZE_5_MS = 5002;
	/** Use 10 ms frames */
	public static final int OPUS_FRAMESIZE_10_MS = 5003;
	/** Use 20 ms frames */
	public static final int OPUS_FRAMESIZE_20_MS = 5004;
	/** Use 40 ms frames */
	public static final int OPUS_FRAMESIZE_40_MS = 5005;
	/** Use 60 ms frames */
	public static final int OPUS_FRAMESIZE_60_MS = 5006;

	public native int create(int sampleRate, int channels, int application);

	public native int encode(short input[], int inOffset, int inSize,
			byte output[], int outOffset, int outSize);

	public native void destroy();

	/**
	 * Gets the encoder's complexity configuration.
	 * 
	 * @return Returns a value in the range 0-10, inclusive.
	 */
	public native int getComplexity();

	/**
	 * Configures the encoder's computational complexity.
	 * 
	 * @param comp
	 *            The supported range is 0-10 inclusive with 10 representing the
	 *            highest complexity.
	 * @return
	 */
	public native int setComplexity(int comp);

	/**
	 * Gets the encoder's bitrate configuration.
	 * 
	 * @return The bitrate in bits per second. The default is determined based
	 *         on the number of channels and the input sampling rate.
	 */
	public native int getBitrate();

	/**
	 * Configures the bitrate in the encoder.
	 * 
	 * @param rate
	 *            Rates from 500 to 512000 bits per second are meaningful, as
	 *            well as the special values OPUS_AUTO and OPUS_BITRATE_MAX. The
	 *            value OPUS_BITRATE_MAX can be used to cause the codec to use
	 *            as much rate as it can, which is useful for controlling the
	 *            rate by adjusting the output buffer size.
	 * 
	 * @return Success or failure
	 */
	public native int setBitrate(int rate);

	/**
	 * Determine if variable bitrate (VBR) is enabled in the encoder.
	 * 
	 * @return Returns one of the following values: 0 Unconstrained VBR. 1
	 *         Constrained VBR (default).
	 */
	public native int getVBR();

	/**
	 * Enables or disables variable bitrate (VBR) in the encoder.
	 * 
	 * The configured bitrate may not be met exactly because frames must be an
	 * integer number of bytes in length.
	 * 
	 * Warning: Only the MDCT mode of Opus can provide hard CBR behavior.
	 * 
	 * @param val
	 *            Allowed values: 0 Hard CBR. For LPC/hybrid modes at very low
	 *            bit-rate, this can cause noticeable quality degradation. 1 VBR
	 *            (default). The exact type of VBR is controlled by
	 *            OPUS_SET_VBR_CONSTRAINT.
	 * @return
	 */
	public native int setVBR(int val);

	/**
	 * Determine if constrained VBR is enabled in the encoder.
	 * 
	 * @return Returns one of the following values:
	 * 
	 *         0 Unconstrained VBR. 1 Constrained VBR (default).
	 */
	public native int getVBRConstraint();

	/**
	 * Enables or disables constrained VBR in the encoder.
	 * 
	 * This setting is ignored when the encoder is in CBR mode.
	 * 
	 * Warning: Only the MDCT mode of Opus currently heeds the constraint.
	 * Speech mode ignores it completely, hybrid mode may fail to obey it if the
	 * LPC layer uses more bitrate than the constraint would have permitted.
	 * 
	 * @param val
	 *            Allowed values:
	 * 
	 *            0 Unconstrained VBR. 1 Constrained VBR (default). This creates
	 *            a maximum of one frame of buffering delay assuming a transport
	 *            with a serialization speed of the nominal bitrate.
	 * @return
	 */
	public native int setVBRConstraint(int val);

	/**
	 * Gets the encoder's forced channel configuration.
	 * 
	 * @return OPUS_AUTO Not forced (default) 1 Forced mono 2 Forced stereo
	 */
	public native int getForceChannels();

	/**
	 * Configures mono/stereo forcing in the encoder.
	 * 
	 * This can force the encoder to produce packets encoded as either mono or
	 * stereo, regardless of the format of the input audio. This is useful when
	 * the caller knows that the input signal is currently a mono source
	 * embedded in a stereo stream.
	 * 
	 * @param val
	 *            Allowed values: OPUS_AUTO Not forced (default) 1 Forced mono 2
	 *            Forced stereo
	 * @return
	 */
	public native int setForceChannels(int val);

	/**
	 * Gets the encoder's configured maximum allowed bandpass.
	 * 
	 * @return Allowed values:
	 * 
	 *         OPUS_BANDWIDTH_NARROWBAND 4 kHz passband
	 *         OPUS_BANDWIDTH_MEDIUMBAND 6 kHz passband OPUS_BANDWIDTH_WIDEBAND
	 *         8 kHz passband OPUS_BANDWIDTH_SUPERWIDEBAND 12 kHz passband
	 *         OPUS_BANDWIDTH_FULLBAND 20 kHz passband (default)
	 */
	public native int getMaxBandwidth();

	/**
	 * Configures the maximum bandpass that the encoder will select
	 * automatically.
	 * 
	 * Applications should normally use this instead of OPUS_SET_BANDWIDTH
	 * (leaving that set to the default, OPUS_AUTO). This allows the application
	 * to set an upper bound based on the type of input it is providing, but
	 * still gives the encoder the freedom to reduce the bandpass when the
	 * bitrate becomes too low, for better overall quality.
	 * 
	 * @param val
	 *            Allowed values: OPUS_BANDWIDTH_NARROWBAND 4 kHz passband
	 *            OPUS_BANDWIDTH_MEDIUMBAND 6 kHz passband
	 *            OPUS_BANDWIDTH_WIDEBAND 8 kHz passband
	 *            OPUS_BANDWIDTH_SUPERWIDEBAND 12 kHz passband
	 *            OPUS_BANDWIDTH_FULLBAND 20 kHz passband (default)
	 * @return
	 */
	public native int setMaxBandwidth(int val);

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
	 * Sets the encoder's bandpass to a specific value.
	 * 
	 * This prevents the encoder from automatically selecting the bandpass based
	 * on the available bitrate. If an application knows the bandpass of the
	 * input audio it is providing, it should normally use
	 * OPUS_SET_MAX_BANDWIDTH instead, which still gives the encoder the freedom
	 * to reduce the bandpass when the bitrate becomes too low, for better
	 * overall quality.
	 * 
	 * @param val
	 *            Allowed values: OPUS_AUTO (default) OPUS_BANDWIDTH_NARROWBAND
	 *            4 kHz passband OPUS_BANDWIDTH_MEDIUMBAND 6 kHz passband
	 *            OPUS_BANDWIDTH_WIDEBAND 8 kHz passband
	 *            OPUS_BANDWIDTH_SUPERWIDEBAND 12 kHz passband
	 *            OPUS_BANDWIDTH_FULLBAND 20 kHz passband
	 * @return
	 */
	public native int setBandwidth(int val);

	/**
	 * Gets the encoder's configured signal type.
	 * 
	 * @return Returns one of the following values:
	 * 
	 *         OPUS_AUTO (default) OPUS_SIGNAL_VOICE Bias thresholds towards
	 *         choosing LPC or Hybrid modes. OPUS_SIGNAL_MUSIC Bias thresholds
	 *         towards choosing MDCT modes.
	 */
	public native int getSignal();

	/**
	 * Configures the type of signal being encoded. This is a hint which helps
	 * the encoder's mode selection.
	 * 
	 * @param sig
	 *            OPUS_AUTO (default) OPUS_SIGNAL_VOICE: Bias thresholds towards
	 *            choosing LPC or Hybrid modes. OPUS_SIGNAL_MUSIC: Bias
	 *            thresholds towards choosing MDCT modes.
	 * @return
	 */
	public native int setSignal(int sig);

	/**
	 * Gets the encoder's configured application.
	 * 
	 * @return Returns one of the following values:
	 * 
	 *         OPUS_APPLICATION_VOIP Process signal for improved speech
	 *         intelligibility. OPUS_APPLICATION_AUDIO Favor faithfulness to the
	 *         original input. OPUS_APPLICATION_RESTRICTED_LOWDELAY Configure
	 *         the minimum possible coding delay by disabling certain modes of
	 *         operation.
	 */
	public native int getApplication();

	/**
	 * Configures the encoder's intended application.
	 * 
	 * The initial value is a mandatory argument to the encoder_create function.
	 * 
	 * @param val
	 *            one of the following values:
	 * 
	 *            OPUS_APPLICATION_VOIP Process signal for improved speech
	 *            intelligibility. OPUS_APPLICATION_AUDIO Favor faithfulness to
	 *            the original input. OPUS_APPLICATION_RESTRICTED_LOWDELAY
	 *            Configure the minimum possible coding delay by disabling
	 *            certain modes of operation.
	 * @return
	 */
	public native int setApplication(int val);

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
	 * Gets the total samples of delay added by the entire codec.
	 * 
	 * This can be queried by the encoder and then the provided number of
	 * samples can be skipped on from the start of the decoder's output to
	 * provide time aligned input and output. From the perspective of a decoding
	 * application the real data begins this many samples late.
	 * 
	 * The decoder contribution to this delay is identical for all decoders, but
	 * the encoder portion of the delay may vary from implementation to
	 * implementation, version to version, or even depend on the encoder's
	 * initial configuration. Applications needing delay compensation should
	 * call this CTL rather than hard-coding a value.
	 * 
	 * @return Total samples of delay added by the entire codec.
	 */
	public native int getLookahead();

	/**
	 * Gets encoder's configured use of inband forward error correction.
	 * 
	 * @return 0: Inband FEC disabled (default). 1: Inband FEC enabled.
	 */
	public native int getInbandFEC();

	/**
	 * Configures the encoder's use of inband forward error correction (FEC).
	 * Note: This is only applicable to the LPC layer
	 * 
	 * @param state
	 *            0: Disable inband FEC (default). 1: Enable inband FEC.
	 * @return Success or failure
	 */
	public native int setInbandFEC(int state);

	/**
	 * Gets the encoder's configured packet loss percentage.
	 * 
	 * @return Returns the configured loss percentage in the range 0-100,
	 *         inclusive (default: 0).
	 */
	public native int getPacketLossPercentage();

	/**
	 * Configures the encoder's expected packet loss percentage.
	 * 
	 * Higher values with trigger progressively more loss resistant behavior in
	 * the encoder at the expense of quality at a given bitrate in the lossless
	 * case, but greater quality under loss.
	 * 
	 * @param perc
	 *            Loss percentage in the range 0-100, inclusive (default: 0).
	 * @return Success or failure
	 */
	public native int setPacketLossPercentage(int perc);

	/**
	 * Gets encoder's configured use of discontinuous transmission.
	 * 
	 * @return Returns one of the following values:
	 * 
	 *         0 DTX disabled (default). 1 DTX enabled.
	 */
	public native int getDTX();

	/**
	 * Configures the encoder's use of discontinuous transmission (DTX).
	 * 
	 * Note: This is only applicable to the LPC layer
	 * 
	 * @param val
	 *            Allowed values: 0 Disable DTX (default). 1 Enabled DTX.
	 * @return
	 */
	public native int setDTX(int val);

	/**
	 * Gets the encoder's configured signal depth.
	 * 
	 * @return Input precision in bits, between 8 and 24 (default: 24).
	 */
	public native int getLsbDepth();

	/**
	 * Configures the depth of signal being encoded.
	 * 
	 * This is a hint which helps the encoder identify silence and near-silence.
	 * 
	 * @param val
	 *            Input precision in bits, between 8 and 24 (default: 24).
	 * @return
	 */
	public native int setLsbDepth(int val);

	/**
	 * Gets the encoder's configured use of variable duration frames.
	 * 
	 * @return Returns one of the following values:
	 * 
	 *         OPUS_FRAMESIZE_ARG Select frame size from the argument (default).
	 *         OPUS_FRAMESIZE_2_5_MS Use 2.5 ms frames. OPUS_FRAMESIZE_5_MS Use
	 *         2.5 ms frames. OPUS_FRAMESIZE_10_MS Use 10 ms frames.
	 *         OPUS_FRAMESIZE_20_MS Use 20 ms frames. OPUS_FRAMESIZE_40_MS Use
	 *         40 ms frames. OPUS_FRAMESIZE_60_MS Use 60 ms frames.
	 *         OPUS_FRAMESIZE_VARIABLE Optimize the frame size dynamically.
	 */
	public native int getExpertFrameDuration();

	/**
	 * Configures the encoder's use of variable duration frames.
	 * 
	 * When variable duration is enabled, the encoder is free to use a shorter
	 * frame size than the one requested in the opus_encode*() call. It is then
	 * the user's responsibility to verify how much audio was encoded by
	 * checking the ToC byte of the encoded packet. The part of the audio that
	 * was not encoded needs to be resent to the encoder for the next call. Do
	 * not use this option unless you really know what you are doing.
	 * 
	 * @param val
	 *            Allowed values:
	 * 
	 *            OPUS_FRAMESIZE_ARG Select frame size from the argument
	 *            (default). OPUS_FRAMESIZE_2_5_MS Use 2.5 ms frames.
	 *            OPUS_FRAMESIZE_5_MS Use 2.5 ms frames. OPUS_FRAMESIZE_10_MS
	 *            Use 10 ms frames. OPUS_FRAMESIZE_20_MS Use 20 ms frames.
	 *            OPUS_FRAMESIZE_40_MS Use 40 ms frames. OPUS_FRAMESIZE_60_MS
	 *            Use 60 ms frames. OPUS_FRAMESIZE_VARIABLE Optimize the frame
	 *            size dynamically.
	 * @return
	 */
	public native int setExpertFrameDuration(int val);

	/**
	 * Gets the encoder's configured prediction status.
	 * 
	 * @return
	 */
	public native int getPredictionDisabled();

	/**
	 * If set to 1, disables almost all use of prediction, making frames almost
	 * completely independent.
	 * 
	 * This reduces quality. (default : 0)
	 * 
	 * @param val
	 * @return
	 */
	public native int setPredictionDisabled(int val);

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
