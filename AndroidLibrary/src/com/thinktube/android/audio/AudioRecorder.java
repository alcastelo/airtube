package com.thinktube.android.audio;

import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.util.Log;
import com.thinktube.audio.AudioConfig;
import com.thinktube.audio.SpeexAEC;
import com.thinktube.codec.AudioEncoderI;

public class AudioRecorder implements Runnable {
	private static final String TAG = "AudioRecorder";

	private final AudioConfig audioConfig;
	private final AudioEncoderI enc;
	private AudioRecord rec;
	private NoiseSuppressor ns;
	private AcousticEchoCanceler aec;
	private AutomaticGainControl agc;
	private SpeexAEC sec;

	public boolean useSpeexAEC = true;
	private volatile boolean running = true;
	private volatile boolean paused = false;
	private short buffer[];
	private int bufferSize;

	private int audioSessionId = -1;

	public AudioRecorder(AudioEncoderI enc, AudioConfig audioConf) {
		this.audioConfig = audioConf;
		this.bufferSize = audioConfig.getFrameBytes();
		this.enc = enc;
		preInit();
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private synchronized void preInit() {
		Log.d(TAG, "initializing");
		final int bufferSize = AudioRecord.getMinBufferSize(audioConfig.sampleRate,
				AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 10;
		Log.d(TAG, "Audio buffer size: " + bufferSize);

		rec = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,
				audioConfig.sampleRate, AudioFormat.CHANNEL_IN_MONO,
				AudioFormat.ENCODING_PCM_16BIT, bufferSize);

		Log.d(TAG, "Audio Source ID: " + rec.getAudioSource() );
		audioSessionId = rec.getAudioSessionId();
		Log.d(TAG, "Audio session ID: " + rec.getAudioSessionId());
		AudioSetup.setAudioSessionId(audioSessionId);

		if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.JELLY_BEAN) {
			Log.d(TAG, "Audio effects not available on this version of Android");
			return;
		}

		if (NoiseSuppressor.isAvailable()) {
			ns = NoiseSuppressor.create(audioSessionId);
			final boolean nsOn = ns.getEnabled();
			Log.d(TAG, "NoiseSuppressor is available" + (nsOn ? " and is " : " but is not") + " enabled by default; enabling.");
			if (!nsOn) {
				ns.setEnabled(true);
			}
			Log.d(TAG, "NoiseSuppressor" + (ns.hasControl()?" has":" doesn't have") + " control");
		} else {
			Log.w(TAG, "NoiseSuppressor is not available");
		}

		// AEC does not work on N7, no matter what it claims
		if (AcousticEchoCanceler.isAvailable() && android.os.Build.MODEL.compareTo("Nexus 7") != 0) {
			aec = AcousticEchoCanceler.create(audioSessionId);
			final boolean aecOn = aec.getEnabled();
			Log.d(TAG, "AcousticEchoCanceler is available" + (aecOn ? " and is" : " but is not") + " enabled by default; enabling.");
			if (! aecOn ) {
				aec.setEnabled(true);
			}
			Log.d(TAG, "AcousticEchoCanceler" + (aec.hasControl()?" has":" doesn't have") + " control");
		} else {
			Log.w(TAG, "AcousticEchoCanceler is not available, using Speex AEC");
			sec = SpeexAEC.getInstance(audioConfig);
		}

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

		if (ns != null && !ns.getEnabled()) {
			Log.e(TAG, "NoiseSuppressor could no be enabled");
		}
		if (aec != null && !aec.getEnabled()) {
			Log.e(TAG, "AcousticEchoCanceler could no be enabled");
		}
		if (agc != null && !agc.getEnabled()) {
			Log.e(TAG, "AutomaticGainControl could no be enabled");
		}
	}

	public synchronized void start() {
		Log.d(TAG, "start");
		running = true;
		new Thread(this, "AudioRecorder").start();
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public synchronized void releaseEffects() {
		if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.JELLY_BEAN) {
			return;
		}

		Log.d(TAG, "release");
		if (aec != null) {
			Log.d(TAG, "releasing AcousticEchoCanceler");
			aec.setEnabled(false);
			aec.release();
			aec = null;
		}
		if (ns != null) {
			Log.d(TAG, "releasing NoiseSuppressor");
			ns.setEnabled(false);
			ns.release();
			ns = null;
		}
		if (agc != null) {
			Log.d(TAG, "releasing AutomaticGainControl");
			agc.setEnabled(false);
			agc.release();
			agc = null;
		}
	}

	public void stop() {
		Log.d(TAG, "stop");
		running = false;
	}

	public byte[] getConfigBytes() {
		enc.start();
		return enc.getConfigBytes();
	}

	public synchronized int getAudioSessionId() {
		return audioSessionId;
	}

	public void setSpeexAEC(boolean on) {
		useSpeexAEC = on;
	}

	public void run() {
		int len;
		buffer = new short[audioConfig.frameSize];

		Log.d(TAG, "Recording with " + bufferSize);

		preInit();

		// trying to improve audio latency:
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

		rec.startRecording();

		/* I'm using the ToneGenerator below for debugging please keep here for a while */

		/*
		com.thinktube.audio.ToneGenerator tg = new com.thinktube.audio.ToneGenerator(160, 8000);
		int c=0;
		*/

		while (running) {
			len = rec.read(buffer, 0, buffer.length);
			if (len != AudioRecord.ERROR_INVALID_OPERATION
					&& len != AudioRecord.ERROR_BAD_VALUE
					&& !paused) {
				/*
				if (c++ <= 3) {
					buffer = tg.makeSound(440*c);
				}
				*/
				if (useSpeexAEC && sec != null) {
					buffer = sec.echoCancel(buffer);
				}
				enc.input(buffer);
			}
		}

		rec.stop();
		rec.release();
		releaseEffects();
		Log.d(TAG, "Stopped recording thread");
	}

	public void setPaused(boolean state) {
		paused = state;
	}
}
