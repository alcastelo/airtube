package com.thinktube.airtube.android.app;

import com.thinktube.airtube.android.AudioService;
import com.thinktube.airtube.android.AirTubeBaseActivity;
import com.thinktube.airtube.android.AirTubeServiceConnection;
import com.thinktube.service.R;

import android.os.Bundle;
import android.view.View;
import android.widget.ToggleButton;

public class AudioSenderActivity extends AirTubeBaseActivity {
	private AudioService as;
	private AirTubeServiceConnection serviceIf;

	@Override	// AirTubeBaseActivity
	protected void start() {
		serviceIf.bind();
	}

	@Override	// AirTubeBaseActivity
	protected void stop() {
		as.stop();
		as.unregister();
		serviceIf.unbind();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_audio_sender);
		as = new AudioService();
		serviceIf = new AirTubeServiceConnection(this, as);
	}

	public void onButtonClicked(View view) {
		boolean on = ((ToggleButton) view).isChecked();
		if (on) {
			start();
		} else {
			stop();
		}
	}

	public void onStartButtonClicked(View view) {
		boolean on = ((ToggleButton) view).isChecked();
		if (on) {
			as.start();
		} else {
			as.stop();
		}
	}
}
