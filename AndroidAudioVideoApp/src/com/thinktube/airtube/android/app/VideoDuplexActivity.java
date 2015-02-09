package com.thinktube.airtube.android.app;

import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;

import com.thinktube.airtube.android.VideoDuplex;
import com.thinktube.airtube.android.AirTubeBaseActivity;
import com.thinktube.airtube.android.AirTubeServiceConnection;
import com.thinktube.android.video.CameraPreview;
import com.thinktube.android.view.ViewUtils;
import com.thinktube.service.R;

public class VideoDuplexActivity extends AirTubeBaseActivity {
	private VideoDuplex vd;
	private AirTubeServiceConnection serviceIf;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video_dup);

		ViewUtils.keepScreenOn(this);

		final SurfaceView videoView = (SurfaceView) findViewById(R.id.decoder);
		videoView.getHolder().addCallback(new SurfaceHolder.Callback() {
			public void surfaceCreated(SurfaceHolder holder) {
				vd.setSurface(holder.getSurface());
			}

			public void surfaceDestroyed(SurfaceHolder holder) {
				vd.stop();
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

		ViewUtils.createToastLogger(this);

		/* video encoding */
		CameraPreview camPreview = (CameraPreview) findViewById(R.id.encoder);
		vd = new VideoDuplex(camPreview);
		camPreview.init(this, 480, 640);
		serviceIf = new AirTubeServiceConnection(this, vd);
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
			vd.start();
		} else {
			vd.stop();
		}
	}

	@Override
	protected void start() {
		serviceIf.bind();
	}

	@Override
	protected void stop() {
		vd.stop();
		vd.unregister();
		serviceIf.unbind();
	}
}
