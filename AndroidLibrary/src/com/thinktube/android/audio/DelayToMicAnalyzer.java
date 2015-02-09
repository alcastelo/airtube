package com.thinktube.android.audio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AutomaticGainControl;
import android.os.Build;
import android.util.Log;

public class DelayToMicAnalyzer implements Runnable {
	private static final String TAG = "MicDelay";
	AudioRecord rec;
	short[] buffer;
	final int bufferSize;
	private boolean running = true;
	short[] shorts = new short[512];
	long timeMic;
	long timeRecv;
	private AutomaticGainControl agc;
	private Object lock = new Object();

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public DelayToMicAnalyzer() {
		bufferSize = AudioRecord.getMinBufferSize(8000,
				AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		Log.d(TAG, "Audio buffer size :" + bufferSize);

		rec = new AudioRecord(MediaRecorder.AudioSource.MIC,
			8000, AudioFormat.CHANNEL_IN_MONO,
			AudioFormat.ENCODING_PCM_16BIT, bufferSize);

		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
			Log.d(TAG, "Audio effects not available on this version of Android");
		} else {
			int audioSessionId = rec.getAudioSessionId();
			Log.d(TAG, "Audio session ID: " + rec.getAudioSessionId());

			if (AutomaticGainControl.isAvailable()) {
				agc = AutomaticGainControl.create(audioSessionId);
				final boolean agcOn = agc.getEnabled();
				Log.d(TAG, "AutomaticGainControl is available" + (agcOn ? " and is" : " but is not") + " enabled by default; enabling.");
				if (! agcOn ) {
					agc.setEnabled(true);
				}
				Log.d(TAG, "AutomaticGainControl" + (agc.hasControl()?" has":" doesn't have") + " control");
			} else {
				Log.w(TAG, "AutomaticGainControl is not available");
			}
		}

		buffer = new short[bufferSize/2];
		new Thread(this, "DelayToMicAnalyzer").start();
	}

	public void receivedPCM(ByteBuffer data) {
		data.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
		analyze(shorts, shorts.length, false);
	}

	@Override
	public void run() {
		int len;

		// trying to improve audio latency:
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

		rec.startRecording();

		while (running) {
			len = rec.read(buffer, 0, buffer.length);
			if (len != AudioRecord.ERROR_INVALID_OPERATION && len != AudioRecord.ERROR_BAD_VALUE) {

				analyze(buffer, len, true);
			}
		}

		rec.stop();
	}

	public void stopRunning() {
		running = false;
	}

	private void analyze(short[] buffer, int len, boolean mic) {
		long timeStart = System.currentTimeMillis();
		int firstLoud = 0;

		for (int i=0; i<len; i++) {
			if (buffer[i] > 15000 && firstLoud == 0) {
				firstLoud = i;
			}
		}

		if (firstLoud != 0) {
			if (mic) {
				Log.d(TAG, "++++++ clap in MIC ");
				synchronized (lock) {
					timeMic = timeStart + firstLoud/8;
				}
			} else {
				long timeRecv = timeStart + firstLoud/8;
				synchronized (lock) {
					Log.d(TAG, "++++++ clap in RECV " + (timeRecv - timeMic));
				}
			}
		}
	}
}
