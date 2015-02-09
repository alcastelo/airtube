package com.thinktube.airtube.android.app;

import com.thinktube.airtube.android.VideoService;
import com.thinktube.airtube.android.AirTubeServiceConnection;
import com.thinktube.android.video.*;
import com.thinktube.android.view.ViewUtils;
import com.thinktube.service.R;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.ToggleButton;

public class VideoSenderActivity extends Activity {
	protected static final String TAG = "VideoSender";

	// working resolutions GN WxH: 640x480, 480x640, 960x720, 1280x720 - (320x240 has problems with the camera preview which stays on 640x480)
	private static final String[] resStrGN = { "640 x 480", /* "480 x 640 (portrait)", */ "960 x 720", "1280 x 720"};
	private static final int[][] resGN = { {640, 480}, /* {480, 640},*/ {960, 720}, {1280, 720}};

	// working resolutions N7 WxH: 320x240, 640x480, 480x640, 960x720, 1280x720
	private static String[] resStr = { "320 x 240", "640 x 480", /* "480 x 640 (portrait)", */ "960 x 720", "1280 x 720"};
	private static int[][] res = { {320, 240}, {640, 480}, /* {480, 640},*/ {960, 720}, {1280, 720}};
	private int resSel = 1;
	private int width, height, fps, sync, bps;
	private static final String[] fpsStr = { "5", "10", "15", "20", "25", "30" };
	private static final String[] syncStr = { "1", "2", "3", "4", "5", "10", "15", "30" };
	private static final String[] bpsStr = { "250k", "500k", "1M", "2M", "3M", "4M", "5M", "10M", "20M" };
	private static final int[] bpsNum = { 250000, 500000, 1000000, 2000000, 3000000, 4000000, 5000000, 10000000, 20000000 };

	private AirTubeServiceConnection airtube;
	CameraPreview camPreview;
	VideoService vs;

	private RelativeLayout ll;
	private Spinner spinnerRes;
	private Spinner spinnerFps;
	private Spinner spinnerIframes;
	private Spinner spinnerBps;

	public VideoSenderActivity() {
		// override resolutions for GN, it does not support 320x240
		if (android.os.Build.MODEL.compareTo("Galaxy Nexus") == 0) {
			resStr = resStrGN;
			res = resGN;
			resSel = 0;
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "onStart()");
		start();
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d(TAG, "onStop()");
		stop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy()");
		stop();
	}

	protected void start() {
		vs = new VideoService(camPreview, width, height, fps, bps, sync);
		camPreview.init(this, width, height);
		camPreview.correctAspectRatio(ll.getWidth(), ll.getHeight());
		enableConfig(false);
		airtube = new AirTubeServiceConnection(this, vs);
		airtube.bind();
	}

	protected void stop() {
		vs.stop();
		vs.unregister();
		airtube.unbind();
		enableConfig(true);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video_sender);

		ViewUtils.keepScreenOn(this);

		ll = (RelativeLayout) findViewById(R.id.layout);

		spinnerRes = (Spinner) findViewById(R.id.spinnerRes);
		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, resStr); 
				//ArrayAdapter.createFromResource(this, R.array.resolutions, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerRes.setAdapter(adapter);

		spinnerRes.setSelection(resSel);
		width = res[resSel][0];
		height = res[resSel][1];

		spinnerRes.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				android.util.Log.d(TAG, "RES pos " + pos + ": " + res[pos][0] + "x" + res[pos][1]);
				width = res[pos][0];
				height = res[pos][1];
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		spinnerFps = (Spinner) findViewById(R.id.spinnerFps);
		ArrayAdapter<CharSequence> adapter2 = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, fpsStr); 
				//ArrayAdapter.createFromResource(this, R.array.resolutions, android.R.layout.simple_spinner_item);
		adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerFps.setAdapter(adapter2);

		spinnerFps.setSelection(2);
		fps = 15;
		spinnerFps.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				android.util.Log.d(TAG, "FPS pos " + pos + ": " + fpsStr[pos]);
				fps = Integer.parseInt(fpsStr[pos]);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		spinnerIframes = (Spinner) findViewById(R.id.spinnerIframes);
		ArrayAdapter<CharSequence> adapter3 = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, syncStr); 
		adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerIframes.setAdapter(adapter3);

		spinnerIframes.setSelection(4);
		sync = 5;
		spinnerIframes.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				android.util.Log.d(TAG, "SYNC pos " + pos + ": " + syncStr[pos]);
				sync = Integer.parseInt(syncStr[pos]);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		spinnerBps = (Spinner) findViewById(R.id.spinnerBps);
		ArrayAdapter<CharSequence> adapter4 = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, bpsStr); 
		adapter4.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerBps.setAdapter(adapter4);

		spinnerBps.setSelection(2);
		bps = 1000000;
		spinnerBps.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				android.util.Log.d(TAG, "BPS pos " + pos + ": " + bpsStr[pos]);
				bps = bpsNum[pos];
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		/* video encoding */
		camPreview = (CameraPreview) findViewById(R.id.encoder);

		ll.post(new Runnable() {
			@Override
			public void run() {
				camPreview.correctAspectRatio(ll.getWidth(), ll.getHeight());
			}
		});

		ViewUtils.createToastLogger(this);
	}

	private void enableConfig(boolean show) {
		spinnerRes.setEnabled(show);
		spinnerFps.setEnabled(show);
		spinnerIframes.setEnabled(show);
		spinnerBps.setEnabled(show);
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
			vs.start();
		} else {
			vs.stop();
		}
	}
}
