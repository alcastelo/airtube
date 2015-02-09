package com.thinktube.audio;

import java.nio.ShortBuffer;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

import com.thinktube.jni.Speex;

public class SpeexAEC {
	private final static Logger LOG = Logger.getLogger(SpeexAEC.class.getSimpleName());
	private static final int MIN_QUEUE = 7;	// for 20ms frames (=160samples/frame)

	// the most common min delay value so far is 128+ ms. ( 20ms/frame = 6.4 frames)
	// taking out the 2 frames Speex uses internally we have about 88ms = 4.4 frames to compensate on the queue.
	// apparently there is a lot of room for setting the queue value:
	// values from 3 to 9 worked in testing, probably 6 being the average should work best.
	private static final int MAX_QUEUE =  2 * MIN_QUEUE;	// shouldn't make sense to store more frames than this number (large delays need large numbers)

	private final int inFrameSize;
	private final int numFramesIn;

	private Speex speex = new Speex();

	private short[] short_capture;
	private short[] input_buffer;
	private short[] echoFree_buffer;

	private Queue<short[]> echoData;
	private Queue<short[]> echoDataFree;

	/**
	 * signals that at least one input frame has come in so queuing can start
	 */
	private volatile boolean inputReady = false;
	private AudioConfig ac;

	private static SpeexAEC instance;

	public synchronized static SpeexAEC getInstance(AudioConfig ac) {
		if (instance == null) {
			instance = new SpeexAEC(ac);
		} else if (!instance.ac.equals(ac)) {
			LOG.warning("Speex AEC already configured with different configuration: " + ac + " this " + ac);
			return null;
		}
		return instance;
	}

	private SpeexAEC(AudioConfig ac) {
		this.ac = ac;
		inFrameSize = ac.getSamplesForMs(20);
		numFramesIn = ac.frameSize / inFrameSize;
		if (ac.frameSize % inFrameSize != 0) {
			LOG.warning("frame size should be a multiple of " + inFrameSize + ", sound will be clipped");
		}

		short_capture = new short[ac.frameSize];
		input_buffer = new short[inFrameSize];
		echoFree_buffer = new short[inFrameSize];

		echoData = new ArrayBlockingQueue<short[]>(MAX_QUEUE);
		echoDataFree = new ArrayBlockingQueue<short[]>(MAX_QUEUE);

		// tail length: longer is *not* better. The longer it is, the longer it takes for the filter to adapt
		// A tail length that is too short will not cancel enough echo.
		int tail = ac.getSamplesForMs(150); // 150ms = 1200 samples @ 8000 Hz // 100 = recommended tail value for a small room

		speex.echoCreate(ac.sampleRate, inFrameSize, tail);
		LOG.info("echo delay queue size " + MIN_QUEUE + " frame size " + ac.frameSize);

		for (int i = 0; i < MAX_QUEUE; i++) {
			echoDataFree.add(new short[inFrameSize]);
		}
	}

	public short[] echoCancel(short[] data) {
		if (echoData.size() < MIN_QUEUE) {
			inputReady = true;
			return data;
		}

		ShortBuffer in = ShortBuffer.wrap(data);
		ShortBuffer out = ShortBuffer.wrap(short_capture);

		for (int i = 0; i < numFramesIn; i++) {
			final short[] echo_buffer = echoData.poll();	// get buffer
			if (echo_buffer == null) {
				break;
			}
			in.get(input_buffer, 0, inFrameSize);
			speex.echoCancelation(input_buffer, echo_buffer, echoFree_buffer, inFrameSize);
			out.put(echoFree_buffer, 0, inFrameSize);

			echoDataFree.add(echo_buffer);	// release buffer
		}
		return short_capture;
	}
	public short[] echoCapture(short[] data) {
		if (echoData.size() < MIN_QUEUE) {
			inputReady = true;
			return data;
		}

		ShortBuffer in = ShortBuffer.wrap(data);
		ShortBuffer out = ShortBuffer.wrap(short_capture);

		for (int i = 0; i < numFramesIn; i++) {
			// TODO: replace speex_echo_playback & speex_echo_capture with speex_echo_cancellation:
			// "When capture and playback are already synchronised, speex_echo_cancellation() is preferable since it gives better control on the exact input/echo timing."
			// by using the queues, we have I/O already synced.
			final short[] output = echoData.poll();	// get buffer
			if (output == null) {
				break;
			}
			speex.echoPlayback(output, inFrameSize);

			in.get(input_buffer, 0, inFrameSize);
			speex.echoCapture(input_buffer, output, inFrameSize);
			out.put(output, 0, inFrameSize);

			echoDataFree.add(output);	// release buffer
		}
		return short_capture;
	}

	public void echoPlayback(short[] data) {
		if ( !inputReady ) {
			LOG.warning("echoPlayback: input is not ready yet");
			return;
		}
		// instead of calling the speex echo playback, just buffer the data: introduce delay to sync input and output
		ShortBuffer in = ShortBuffer.wrap(data);

		for (int i = 0; i < numFramesIn; i++) {
			final short[] input = echoDataFree.poll();// get buffer
			if (input != null) {
				in.get(input, 0, inFrameSize);
				echoData.add(input);	// queue buffer
			} else {
				LOG.warning("echoPlayback: no buffer available ");
			}
		}
	}

	/**
	 * to be called by user when the Audio source (recorder) changes or is restarted.
	 * Restarting the Audio player does not affect the filter.
	 */
	public void resetBuffers() {
		inputReady = false;
		echoDataFree.addAll(echoData);
		echoData.clear();
	}
}
