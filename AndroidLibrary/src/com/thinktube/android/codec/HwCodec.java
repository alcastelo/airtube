package com.thinktube.android.codec;

import java.nio.ByteBuffer;
import com.thinktube.android.codec.HwCodecImplFactory.HwCodecImplI;
import com.thinktube.codec.Codec;
import com.thinktube.codec.CodecOutCallbackI;
import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
abstract class HwCodec extends Codec {
	protected String TAG = "HwCodec";
	private final boolean DEBUG = false;
	MediaCodec mediaCodec;
	ByteBuffer[] inputBuffers;
	ByteBuffer[] outputBuffers;
	int maxInputSize;
	CodecOutThread codecOutThread;
	boolean ready = false;
	int framesIn = 0;
	int framesOut = 0;

	/* Sequential input/output, which means reading the output directly after
	 * putting data into the encoder seems the better way to drive the
	 * MediaCodec in general.
	 * But there are exceptions, when it is better to read the output in a
	 * thread: For example the AAC encoder which may generate more than one
	 * output frame in response to one input frame. Or when the frame input
	 * happens on the main thread. */
	protected boolean useThread = false;

	/* let subclasses override the timeout for input and output buffers
	 * default (-1) is to block */
	protected long timeout = -1;

	/* this really is for the encoder but easier to keep here */
	byte[] configBytes = null;

	// TODO: probably best if injected
	HwCodecImplI codecImpl = HwCodecImplFactory.createCodec();

	protected HwCodec(CodecOutCallbackI cb) {
		super(cb);
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
			Log.e(TAG, "MediaCodec not available in this version of Android! Expect errors!");
		}
	}

	public void start() {
		mediaCodec.start();
		inputBuffers = mediaCodec.getInputBuffers();
		outputBuffers = mediaCodec.getOutputBuffers();
		maxInputSize = inputBuffers[0].limit();

		framesIn = 0;
		framesOut = 0;

		if (useThread) {
			codecOutThread = new CodecOutThread(this);
			codecOutThread.start();
		}

		ready = true;
		Log.d(TAG, "codec started");
	}

	public void stop() {
		ready = false;
		if (codecOutThread != null) {
			codecOutThread.stopRunning();
			codecOutThread = null;
		}

		if (mediaCodec != null) {
			try {
				mediaCodec.flush();
				mediaCodec.stop();
				mediaCodec.release();
			} catch (IllegalStateException e) {
				Log.d(TAG, "illegal codec state");
			}
		}
		super.callbackStop();
		Log.d(TAG, "codec stopped");
	}

	public void input(byte[] data) {
		input(data, 0, data.length, 0);
	}

	public void input(byte[] data, int offset, int len, int flags) {
		//long time = System.currentTimeMillis();
		_input(data, offset, len, flags);
		if (!useThread)
			output(timeout, null);
		//Log.d(TAG, "Encode took " + (System.currentTimeMillis()-time));
	}

	public void _input(byte[] data, int offset, int len, int flags) {
		if (len <= 0 || len > maxInputSize) {
			Log.d(TAG, "input data too large or too small: " + len + " vs " + maxInputSize);
			return;
		}
		long startTime = System.currentTimeMillis();
		try {
			// we drop input frames if we don't get an input buffer within timeout
			int index = mediaCodec.dequeueInputBuffer(timeout);
			if (index >= 0) {
				framesIn++;
				if (DEBUG) Log.d(TAG, "codec in #" + framesIn + ": len " + len);
				inputBuffers[index].clear();
				inputBuffers[index].put(data, offset, len);
				mediaCodec.queueInputBuffer(index, 0, len, 100, flags);
			} else {
				Log.d(TAG, "*** dropped input frame! timeout " + timeout + " in/out: " + framesIn + "/" + framesOut + " = " + (framesIn - framesOut));
			}
		} catch (IllegalStateException e) {
			Log.d(TAG, "illegal codec state in");
		}
		// Log.d(TAG, "frames in - out " + framesIn + " - " + framesOut + " = " + (framesIn - framesOut));
		if (DEBUG) Log.d(TAG, "input took " + (System.currentTimeMillis() - startTime) + "ms");
	}

	/** if to != null copy output to the ByteBuffer provided */
	public int output(long timeout, ByteBuffer to) {
		int index = -100;
		long startTime = System.currentTimeMillis();
		MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
		try {
			index = mediaCodec.dequeueOutputBuffer(info, timeout);
			if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
				MediaFormat format = mediaCodec.getOutputFormat();
				Log.d(TAG, "format changed " + format);
				this.callbackFormatChanged(format);
				/* This happens on Android 4.3: the first time we dequeue the output buffer  we get
				 * "format changed", the next time we get the config bytes, as on Android 4.2 */
				index = mediaCodec.dequeueOutputBuffer(info, timeout);
			}
			/* this is encoder only but easier to keep here:
			 * We save the config bytes and then direcly dequeue the next frame */
			if (index >= 0 && info.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
				Log.d(TAG, "got config bytes, len " + info.size);
				if (outputBuffers[index] != null) {
					configBytes = new byte[info.size];
					outputBuffers[index].get(configBytes);

					/* call callback as well, some need the config bytes */
					this.callOutCallback(outputBuffers[index], info);
					outputBuffers[index].clear();
				}

				mediaCodec.releaseOutputBuffer(index, false);

				/* dequeue again, but with timeout 1ms */
				index = mediaCodec.dequeueOutputBuffer(info, timeout);
			}
			if (index >= 0) {
				framesOut++;
				if (DEBUG) Log.d(TAG, "codec out #" + framesOut + ": size " + info.size + " time " + info.presentationTimeUs);

				/* note that if a surface was provided in the codec config for a
				 * video decoder, info.size will be 0 or outputBuffers[index] null
				 * and the data is not accessible - instead the "true" of
				 * releaseOutputBuffer will render it to the surface */

				if (info.size > 0 && outputBuffers[index] != null) {
					if (to != null) {
						outputBuffers[index].limit(to.limit());
						to.put(outputBuffers[index]);
						to.flip();
						outputBuffers[index].rewind();
					}

					this.callOutCallback(outputBuffers[index], info);

					outputBuffers[index].clear();
				}

				mediaCodec.releaseOutputBuffer(index, true);

				if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
					Log.d(TAG, "EOF");
				}
			} else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
				Log.d(TAG, "buffers changed");
				outputBuffers = mediaCodec.getOutputBuffers();
			} else if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
				Log.d(TAG, "codec out timeout");
			} else {
				Log.d(TAG, "unknown codec out");
			}
		} catch (IllegalStateException e) {
			Log.d(TAG, "illegal codec state out");
		}
		if (DEBUG) Log.d(TAG, "output took " + (System.currentTimeMillis() - startTime) + "ms");
		return index;
	}

	public boolean isReady() {
		return ready;
	}

	protected void callOutCallback(ByteBuffer bb, MediaCodec.BufferInfo info) {
		if (outCb != null && outCb instanceof CodecOutCallbackAndroidI) {
			((CodecOutCallbackAndroidI)outCb).handleFrame(bb, info);
		}
	}

	public void callbackFormatChanged(MediaFormat format) {
		if (outCb != null && outCb instanceof CodecOutCallbackAndroidI) {
			((CodecOutCallbackAndroidI)outCb).formatChanged(format);
		}
	}
}

class CodecOutThread extends Thread
{
	private final HwCodec codec;
	private volatile boolean running = true;

	public CodecOutThread(HwCodec c) {
		setDaemon(true);
		setName("CodecOutThread");
		codec = c;
	}

	@Override
	public void run() {
		// trying to improve latencies
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		Log.d(codec.TAG, "output thread running");

		while (running) {
			codec.output(-1, null);
		}
	}

	public void stopRunning() {
		running = false;
	}
}
