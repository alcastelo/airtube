package com.thinktube.android.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.thinktube.audio.AudioConfig;
import com.thinktube.audio.NoiseGenerator;
import com.thinktube.audio.SpeexAEC;

/**
 * AudioPlayer is a wrapper around AudioTrack in "streaming" mode to make it
 * more robust in the case when we can't input data fast enough (packet loss).
 * 
 * It runs a tight loop where it tries to get the next frame from AudioReceiver
 * and writes it to the track. When no frame is available it will play out equal
 * length "comfort noise" instead. Since AudioTrack.write blocks when the track
 * buffer is full, this guarantees that the frames are received from the
 * AudioReceiver and written in average in "frame size" (e.g. 20ms) intervals.
 * 
 * Note: Using an OnPlaybackPositionUpdateListener of the AudioTrack was not
 * sufficient to guarantee correct timing when using 20ms frame sizes (For 60ms
 * intervals it worked).
 * 
 * Also the AudioPlayer uses Speex AEC when necessary and available so it
 * integrates with AudioRecorder.
 * 
 * @author br1
 */
public class AudioPlayer implements Runnable {
	private static final String TAG = "AudioPlayer";

	private final AudioTrack track;
	private final AudioReceiver recv;
	private final NoiseGenerator gen;
	private final short[] buffer;
	private SpeexAEC sec;
	private boolean useSpeexAEC = true;
	private volatile boolean running = true;
	private long framesWritten=0;

	AudioPlayer(int sessionId, AudioConfig audioConfig, AudioReceiver recv) {
		this.recv = recv;

		/*
		 * AudioTrack buffer size calculation:
		 * We receive chunks of audioConfig.getSampleBufferBytes()
		 * The track needs at least twice than that to ensure playback without clicks.
		 * Also we need to respect the minBufferSize of the track.
		 */
		int trackMinBufferSize = AudioTrack.getMinBufferSize(audioConfig.sampleRate,
				AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
		int recvBufferSize = audioConfig.getFrameBytes() * 2;
		int audioBufferSize = Math.max(trackMinBufferSize, recvBufferSize) * 2;
		Log.d(TAG, "AudioTrack bufferSize = " + audioBufferSize + " minBufferSize = " + trackMinBufferSize);

		if (sessionId >= 0) {
			Log.d(TAG, "Creating Audio track with session id = " + sessionId);
			track = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
					audioConfig.sampleRate, AudioFormat.CHANNEL_OUT_MONO,
					AudioFormat.ENCODING_PCM_16BIT, audioBufferSize,
					AudioTrack.MODE_STREAM, sessionId);
		} else {
			track = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
					audioConfig.sampleRate, AudioFormat.CHANNEL_OUT_MONO,
					AudioFormat.ENCODING_PCM_16BIT, audioBufferSize,
					AudioTrack.MODE_STREAM);
		}

		gen = new NoiseGenerator(audioConfig.frameSize);

		buffer = new short[audioConfig.frameSize];

		if (android.os.Build.MODEL.compareTo("Nexus 7") == 0) {
			sec = SpeexAEC.getInstance(audioConfig);
		}

		Log.d(TAG, "Initialized AudioPlayer with sessionId " + track.getAudioSessionId() + " and " + audioConfig);
	}

	public synchronized void start() {
		running = true;
		new Thread(this, "AudioPlayer").start();
	}

	public synchronized void stop() {
		running = false;
	}

	private void writeToTrack(short[] buffer) {
		framesWritten += track.write(buffer, 0, buffer.length);

		if (useSpeexAEC && sec != null) {
			sec.echoPlayback(buffer);
		}
	}

	@Override
	public void run() {
		// trying to improve audio latency:
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

		Log.d(TAG, "Playback started");
		track.play();

		while (running) {
			boolean success = recv.getAudioFrame(buffer);
			if (success) {
				/* got audio data from receiver. In case of packet loss it may
				 * well be silence but we'll just trust the decoder */
				writeToTrack(buffer);
			} else {
				/* no sound data available, substitute with "comfort noise" */
				Log.d(TAG, "buffer underrun, feeding silence");
				writeToTrack(gen.makeSound());
			}
		}

		track.stop();
		Log.d(TAG, "Playback ended");
	}

	public void setSpeexAEC(boolean on) {
		useSpeexAEC = on;
	}

	public long getBufferedFrames() {
		return framesWritten - track.getPlaybackHeadPosition();
	}
}
