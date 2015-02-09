package com.thinktube.airtube.android.app;

import com.thinktube.airtube.android.AudioClient;
import com.thinktube.airtube.android.AirTubeBaseActivity;
import com.thinktube.airtube.android.AirTubeServiceConnection;
import com.thinktube.android.audio.AudioSetup;
import com.thinktube.audio.JitterBuffer.Statistics;
import com.thinktube.service.R;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

public class AudioReceiverActivity extends AirTubeBaseActivity {
	TextView stats;
	AudioClient ac;
	private AirTubeServiceConnection conn;
	private AudioSetup as;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_audio_receiver);
		stats = (TextView) findViewById(R.id.stats);
		ac = new AudioClient();
		conn = new AirTubeServiceConnection(this, ac);

		stats.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (ac != null) {
					Statistics s = ac.getStats();
					if (s != null)
						stats.setText("Received packets:\t\t\t\t" + s.receivedPackets + "\n" +
									"Late packets:\t\t\t\t\t\t\t" + s.latePackets + "\n" +
									"Duplicate packets:\t\t\t\t" + s.duplicatePackets + "\n" +
									"Lost packets:\t\t\t\t\t\t\t" + s.lostPackets + "\n" +
									"Played packets:\t\t\t\t\t\t" + s.playedPackets + "\n" +									
									"Play buffer underruns:\t" + s.bufferUnderruns + "\n" +
									"Current buffers:\t\t\t\t\t\t" + s.buffersInUse + "\n" +
									"Max delay:\t\t\t\t\t\t\t\t\t" + s.maxDelay + "\n" +
									"Min delay:\t\t\t\t\t\t\t\t\t" + s.minDelay + "\n" +
									"Average delay:\t\t\t\t\t\t" + (s.receivedPackets > 0 ? s.sumDelay/s.receivedPackets : 0)
								);
					stats.postDelayed(this, 1000);
				}
			}
		}, 1000);

		/*  Audio setup */
		as = new AudioSetup(this);
		as.setup();
		as.useSpeaker(true);
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
			ac.start();
		} else {
			ac.stop();
		}
	}

	public void onSpeakerButtonClicked(View view) {
		boolean on = ((ToggleButton) view).isChecked();
		as.useSpeaker(on);
	}

	@Override	// AirTubeBaseActivity
	protected void start() {
		conn.bind();
	}

	@Override	// AirTubeBaseActivity
	protected void stop() {
		ac.stop();
		ac.unregister();
		conn.unbind();
		as.useSpeaker(false);
		as.teardown();
	}
}
