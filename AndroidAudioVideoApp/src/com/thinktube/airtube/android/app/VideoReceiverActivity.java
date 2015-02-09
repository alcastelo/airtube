package com.thinktube.airtube.android.app;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.thinktube.airtube.ConfigParameters;
import com.thinktube.airtube.android.VideoClient;
import com.thinktube.airtube.android.AirTubeBaseActivity;
import com.thinktube.airtube.android.AirTubeServiceConnection;
import com.thinktube.android.view.ViewUtils;
import com.thinktube.service.R;

import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

public class VideoReceiverActivity extends AirTubeBaseActivity {
	private VideoClient vc;
	private AirTubeServiceConnection clientIf;

	private RelativeLayout ll;
	private SurfaceView videoView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video_receiver);

		ViewUtils.keepScreenOn(this);

		videoView = (SurfaceView) findViewById(R.id.decoder);
		videoView.getHolder().addCallback(new SurfaceHolder.Callback() {
			public void surfaceCreated(SurfaceHolder holder) {
				vc.setSurface(holder.getSurface());
			}

			public void surfaceDestroyed(SurfaceHolder holder) {
				vc.stop();
			}

			public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
				// nothing
			}
		});

		ll = (RelativeLayout) findViewById(R.id.layout);
		ll.post(new Runnable() {
			@Override
			public void run() {
				setAspectRatio(640, 480);
			}
		});

		Logger.getLogger("").addHandler(new Handler() {
			@Override
			public void publish(LogRecord rec) {
				if (isLoggable(rec)) {
					Toast.makeText(VideoReceiverActivity.this, rec.getMessage(), Toast.LENGTH_SHORT).show();
				}
			}

			@Override
			public void close() {
			}

			@Override
			public void flush() {
			}
		});

		vc = new VideoClient();
		vc.setCallback(new VideoClient.VideoClientCB() {
			@Override
			public void onSubscribe(ConfigParameters cp) {
				try {
					int w = cp.getInt("width");
					int h = cp.getInt("height");
					setAspectRatio(w, h);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		clientIf = new AirTubeServiceConnection(this, vc);
	}

	protected void setAspectRatio(int videoWidth, int videoHeight) {
		LayoutParams lp = videoView.getLayoutParams();

		int screenWidth = ll.getWidth();
		int screenHeight = ll.getHeight();
		android.util.Log.d("XXX", "SPACE: " + screenWidth + "x" + screenHeight);

		lp.height = (int) (((float) videoHeight / (float) videoWidth) * (float) screenWidth);
		lp.width = screenWidth;

		if (lp.height > screenHeight) {
			lp.height = screenHeight;
			lp.width = (int) (((float) videoWidth / (float) videoHeight) * (float) lp.height);
		}

		android.util.Log.d("XXX", "FINALLY: " + lp.width + "x" + lp.height + " = " + ((float) lp.width) / lp.height);
		videoView.setLayoutParams(lp);
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
			vc.start();
		} else {
			vc.stop();
		}
	}

	@Override	// AirTubeBaseActivity
	protected void start() {
		clientIf.bind();
	}

	@Override	// AirTubeBaseActivity
	protected void stop() {
		vc.stop();
		vc.unregister();
		clientIf.unbind();
	}
}
