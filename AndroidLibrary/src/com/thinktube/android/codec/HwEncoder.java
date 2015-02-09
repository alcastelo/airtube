package com.thinktube.android.codec;

import com.thinktube.codec.CodecOutCallbackI;
import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public abstract class HwEncoder extends HwCodec {
	protected int expectedInputSize;

	protected HwEncoder(CodecOutCallbackI cb) {
		super(cb);
	}

	public abstract void init();

	public void start() {
		super.start();
		initialInput(); // TODO: this does not work with video encoding from surface!
	}

	/* this initially inputs an empty buffer to the encoder, which has several consequences,
	 * which are all handled in output()
	 *   1) get format changed
	 *   2) get config bytes (-> we need these!)
	 *   3) get an initial output data (what does it contain?)
	 */
	public void initialInput() {
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
			Log.e(TAG, "MediaCodec not available in this version of Android! Expect errors!");
			return;
		}

		Log.d(TAG, "initial input of " + expectedInputSize);
		try {
			/* input nothing */
			int index = mediaCodec.dequeueInputBuffer(-1);
			if (index < 0) {
				Log.d(TAG, "*** getConfig: dropped input frame");
				return;
			}
			framesIn++;
			inputBuffers[index].clear();
			//mediaCodec.queueInputBuffer(index, 0, inputBuffers[index].limit(), 100, 0);
			mediaCodec.queueInputBuffer(index, 0, expectedInputSize, 100, 0);
			//mediaCodec.queueInputBuffer(index, 0, 0, 100, 0); // this does not work on N7

			if (!useThread) {
				output(timeout, null);
			}
			Log.d(TAG, "initital input finished");
		} catch (IllegalStateException e) {
			Log.d(TAG, "getConfig: illegal codec state");
		}
	}

	public byte[] getConfigBytes() {
		return configBytes;
	}
}
