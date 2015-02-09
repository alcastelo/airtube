package com.thinktube.airtube.android.app;

import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;

import com.thinktube.airtube.android.AudioVideoDuplex;
import com.thinktube.airtube.android.AirTubeBaseActivity;
import com.thinktube.airtube.android.AirTubeServiceConnection;
import com.thinktube.android.audio.AudioSetup;
import com.thinktube.android.video.CameraPreview;
import com.thinktube.android.view.ViewUtils;
import com.thinktube.service.R;

public class VideoCallActivity extends AirTubeBaseActivity {
	private AudioSetup as;
	private AudioVideoDuplex avd;
	private AirTubeServiceConnection serviceIf;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video_call);

		ViewUtils.keepScreenOn(this);

		final SurfaceView videoView = (SurfaceView) findViewById(R.id.decoder);
		videoView.getHolder().addCallback(new SurfaceHolder.Callback() {
			public void surfaceCreated(SurfaceHolder holder) {
				avd.setSurface(holder.getSurface());
			}

			public void surfaceDestroyed(SurfaceHolder holder) {
				avd.stop();
			}

			public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
				// nothing
			}
		});

		final RelativeLayout ll = (RelativeLayout) findViewById(R.id.layout);
		ll.post(new Runnable() {
			@Override
			public void run() {
				ViewUtils.setAspectRatio(videoView, 480, 640, ll.getWidth(), ll.getHeight());
			}
		});

		CameraPreview camPreview = (CameraPreview) findViewById(R.id.encoder);
		avd = new AudioVideoDuplex(camPreview);
		camPreview.init(this, 480, 640);
		serviceIf = new AirTubeServiceConnection(this, avd);

		/*  Audio setup */
		as = new AudioSetup(this);
		as.setup();

		ViewUtils.createToastLogger(this);
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
			avd.start();
		} else {
			avd.stop();
		}
	}

	public void onSpeakerButtonClicked(View view) {
		boolean on = ((ToggleButton) view).isChecked();
		as.useSpeaker(on);
	}

	@Override
	protected void start() {
		serviceIf.bind();
		as.setup();
		as.useSpeaker(true);
	}

	@Override
	protected void stop() {
		avd.stop();
		avd.unregister();
		serviceIf.unbind();
		as.teardown();
	}
}
