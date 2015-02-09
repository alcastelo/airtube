package com.thinktube.airtube.android.app;

import android.os.Bundle;
import android.view.View;
import android.widget.ToggleButton;

import com.thinktube.airtube.test.TestServiceClient;
import com.thinktube.airtube.testservice.R;
import com.thinktube.airtube.android.AirTubeBaseActivity;
import com.thinktube.airtube.android.AirTubeServiceConnection;
import com.thinktube.android.view.LoggerScrollView;

import android.util.Log;

public class TestClientActivity extends AirTubeBaseActivity {
	protected static final String TAG = "TestClientAct";
	private TestServiceClient test;
	private AirTubeServiceConnection airtube;
	private LoggerScrollView log;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate()");
		setContentView(R.layout.activity_client);
		log = (LoggerScrollView) findViewById(R.id.scroller);

		test = new TestServiceClient();
		airtube = new AirTubeServiceConnection(this, test);
	}

	protected void start() {
		Log.d(TAG, "start");
		log.clear();
		airtube.bind();
	}

	protected void stop() {
		Log.d(TAG, "stop");
		test.unregister();
		airtube.unbind();
	}

	public void onButtonClicked(View view) {
		boolean on = ((ToggleButton) view).isChecked();
		if (on) {
			start();
		} else {
			stop();
		}
	}

	public void onTestClicked(View view) {
		test.testSend();
	}
}
