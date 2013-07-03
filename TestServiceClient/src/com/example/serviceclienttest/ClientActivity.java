package com.example.serviceclienttest;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.thinktube.airtube.ClientCallback;
import com.thinktube.airtube.ClientInterface;
import com.thinktube.airtube.ServiceData;

public class ClientActivity extends Activity {
	protected static final String TAG = "CCC";
	private TextView text;
	private ScrollView scroller;
	private int myId;
	Intent intent = new Intent("AirTube_client");
	private ClientInterface clientIf;

	private ClientCallback.Stub clientCb = new ClientCallback.Stub() {
		@Override
		public void receiveData(ServiceData data) throws RemoteException {
			log("received " + data.test);
		}
	};

	private ServiceConnection serviceConn = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			clientIf = ClientInterface.Stub.asInterface(service);
			log("connected");

			try {
				myId = clientIf.subscribeService("test", clientCb);
				log("subscribed");

			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			log("Service has unexpectedly disconnected");
			clientIf = null;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_client);
		text = (TextView) findViewById(R.id.text);
		scroller = (ScrollView) findViewById(R.id.scroller);
		
		startService(intent);
		start();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		stop();
	}

	private void start() {
		bindService(intent, serviceConn, 0);
	}
	
	private void stop() {
		try {
			clientIf.unsubscribeService("test", myId);
			log("unsubscribed");
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		unbindService(serviceConn);
		log("unbound");
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
