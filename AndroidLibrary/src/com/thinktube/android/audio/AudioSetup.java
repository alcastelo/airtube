package com.thinktube.android.audio;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Build;
import android.util.Log;

public class AudioSetup implements OnAudioFocusChangeListener {

	private final static String TAG = "AudioSetup";
	private Activity activity;
	private AudioManager am;
	public AudioSetup(Activity activity) {
		this.activity = activity;

	}

	static private int audioSessionId = -1;

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	public void setup() {

		activity.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
		am = (AudioManager) activity.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
		int result = am.requestAudioFocus(this,
		                                 AudioManager.STREAM_VOICE_CALL,	// for VoIP like
		                                 AudioManager.AUDIOFOCUS_GAIN); // Permanent focus.

		if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
			Log.d(TAG, "AUDIOFOCUS_REQUEST_GRANTED");

		} else {
			Log.d(TAG, "AUDIOFOCUS_REQUEST_GRANTED NOT!!!!");
		}

		// setMode to MODE_IN_COMMUNICATION or don't, but never to MODE_IN_CALL
		am.setMode(AudioManager.MODE_IN_COMMUNICATION);

		// default app volumen to half to avoid initial audio feedback
		final int midVol= am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)/2;
		am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, midVol, AudioManager.FLAG_SHOW_UI + AudioManager.FLAG_ALLOW_RINGER_MODES);

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) { // API level 17
			Log.d(TAG, "PROPERTY_OUTPUT_FRAMES_PER_BUFFER: " + am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER));
			Log.d(TAG, "PROPERTY_OUTPUT_SAMPLE_RATE: " + am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE));
		}
	}

	public void teardown() {
		am.setMode(AudioManager.MODE_NORMAL);
		activity.setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
		am.abandonAudioFocus(this);
	}

	public void useSpeaker(boolean speaker) {
		Log.d(TAG, "Before: isSpeakerphoneOn = "+ am.isSpeakerphoneOn());
		am.setSpeakerphoneOn(speaker);
		Log.d(TAG, "After: isSpeakerphoneOn = "+ am.isSpeakerphoneOn());

	}

	public void setMicrophoneMute(boolean on) {
		am.setMicrophoneMute(on);
	}

	int curVolLevel;
	public void setSpeakerMute(boolean on) {
		if (on) {
			curVolLevel = am.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
			am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, 0, 0);
		} else {
			am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, curVolLevel, 0);
		}
		Log.d(TAG, "setSpeakerMute() = " + on);
	}

	@Override
	public void onAudioFocusChange(int focusChange) {
		 if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
			 Log.d(TAG, "onAudioFocusChange(): AUDIOFOCUS_LOSS");
		 }

	}

	public static void setAudioSessionId(int id) {
		audioSessionId = id;
		Log.d(TAG, "Audio session ID: " + audioSessionId);
	}
	public static int getAudioSessionId() {
		return audioSessionId;
	}
}
