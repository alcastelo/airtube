package com.thinktube.airtube.android;

import android.util.Log;

import android.app.Activity;
import android.os.Bundle;

public abstract class AirTubeBaseActivity extends Activity {
	private static final String TAG = "AirTubeBaseActivity";
	private boolean restarting = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Log.d(TAG, "onRestart()");
		restarting  = true;
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "onStart()");
		if (restarting) {
			restarting = false;
			Log.d(TAG, "skipping onStart()");
		} else {
			start();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "onPause()");
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d(TAG, "onStop()");
	}

	/**
	 * this in reality seldom gets called by the system
	 * it's more of help debugging, as finishing the app via the back button triggers it.
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy()");
		stop();
	}

	protected abstract void start();
	protected abstract void stop();
}
