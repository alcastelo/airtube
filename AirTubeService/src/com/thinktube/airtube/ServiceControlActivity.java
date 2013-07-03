package com.thinktube.airtube;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

public class ServiceControlActivity extends Activity {
	protected static final String TAG = "SSS";
	Intent intent = new Intent("AirTube_server");
	private TextView text;
	private ScrollView scroller;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		text = (TextView) findViewById(R.id.text);
		scroller = (ScrollView) findViewById(R.id.scroller);

		startService(intent);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	public void onButtonClicked(View view) {
		boolean on = ((ToggleButton) view).isChecked();
		if (on) {
			startService(intent);
			log("started");
		} else {
			stopService(intent);
			log("stopped");
		}
	}
	
	private void log(final String msg) {
		Log.d(TAG, msg);
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				text.append(msg + "\n");
				scroller.smoothScrollTo(0, text.getBottom());
			}
		});
	}
}
