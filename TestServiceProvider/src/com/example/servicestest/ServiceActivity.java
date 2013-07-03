package com.example.servicestest;

import java.util.Timer;
import java.util.TimerTask;

import com.thinktube.airtube.*;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

public class ServiceActivity extends Activity {
	protected static final String TAG = "SSS";

	ServiceDescription myServ = new ServiceDescription("test");
	int myId;
	Intent intent = new Intent("AirTube_server");
	private ServiceInterface serviceIf;
	private Timer timer;
	TextView text;
	ScrollView scroller;

	private ServiceConnection serviceConn = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			serviceIf = ServiceInterface.Stub.asInterface(service);

			log("connected to service");

			try {
				myId = serviceIf.registerService(myServ);
				log("registered myself");

				timer = new Timer("Service Test Timer");
				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						if (serviceIf == null)
							return;

						ServiceData sd = new ServiceData();
						sd.test = "bla";
						try {
							log("send " + sd.test);
							serviceIf.sendData(myId, sd);
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}
					}, 1000L, 10 * 1000L);

			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			log("service has disconnected");
			serviceIf = null;
			stop();
		}
	};

	private void start() {
		bindService(intent, serviceConn, 0);
		log("started");
	}

	private void stop() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		if (serviceIf != null) {
			try {
				serviceIf.unregisterService(myId);
				log("unregistered myself");
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log("err");
			}
		}
		unbindService(serviceConn);
		log("stopped");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		text = (TextView) findViewById(R.id.text);
		scroller = (ScrollView) findViewById(R.id.scroller);

		startService(intent);
	}

	@Override
	protected void onStart() {
		super.onStart();
		start();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		stop();
	}

	public void onButtonClicked(View view) {
		boolean on = ((ToggleButton) view).isChecked();
		if (on) {
			start();
		} else {
			stop();
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
