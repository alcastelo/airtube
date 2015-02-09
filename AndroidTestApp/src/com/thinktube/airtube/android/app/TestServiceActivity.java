package com.thinktube.airtube.android.app;

import android.os.Bundle;

import com.thinktube.airtube.TrafficClass;
import com.thinktube.airtube.TransmissionType;
import com.thinktube.airtube.test.TestServiceProvider;
import com.thinktube.airtube.testservice.R;
import com.thinktube.airtube.android.AirTubeBaseActivity;
import com.thinktube.airtube.android.AirTubeServiceConnection;
import com.thinktube.android.view.LoggerScrollView;

import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.ToggleButton;

public class TestServiceActivity extends AirTubeBaseActivity {
	protected static final String TAG = "TestServiceAct";
	private TestServiceProvider test;
	private AirTubeServiceConnection airtube;
	private LoggerScrollView log;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate()");
		setContentView(R.layout.activity_service);
		log = (LoggerScrollView) findViewById(R.id.scroller);

		test = new TestServiceProvider();
		airtube = new AirTubeServiceConnection(this, test);
	}

	protected void start() {
		Log.d(TAG, "start");
		log.clear();
		airtube.bind();
	}

	protected void stop() {
		Log.d(TAG, "stop");
		test.stop();
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

	public void onRadioButtonClicked(View view) {
		boolean checked = ((RadioButton) view).isChecked();

		if (checked) {
			switch (view.getId()) {
			case R.id.radio_udp:
				test.setTransmissionType(TransmissionType.UDP);
				break;
			case R.id.radio_tcp:
				test.setTransmissionType(TransmissionType.TCP);
				break;
			case R.id.radio_bcast:
				test.setTransmissionType(TransmissionType.UDP_BROADCAST);
				break;
			}
		}
	}

	public void onTOSRadioClicked(View view) {
		boolean checked = ((RadioButton) view).isChecked();

		if (checked) {
			switch (view.getId()) {
			case R.id.radio_background:
				test.setTrafficClass(TrafficClass.BACKGROUND);
				break;
			case R.id.radio_normal:
				test.setTrafficClass(TrafficClass.NORMAL);
				break;
			case R.id.radio_video:
				test.setTrafficClass(TrafficClass.VIDEO);
				break;
			case R.id.radio_voice:
				test.setTrafficClass(TrafficClass.VOICE);
				break;
			}
		}
	}

	public void onPeriodicButtonClicked(View view) {
		boolean on = ((CheckBox) view).isChecked();
		test.periodicMessages(on);
	}

	public void onTestClicked(View view) {
		test.testSend();
	}
}
