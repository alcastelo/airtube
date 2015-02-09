package com.thinktube.airtube.android;

import com.thinktube.airtube.AirTube;
import android.util.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

public class ConnectivityBroadcastReceiver extends BroadcastReceiver {
	private static final String TAG = "AirTubeBcastReceiver";
	private boolean connectedWIFI = false;
	private boolean connected3G = false;

	private AirTube airtube;
	private WifiManager.MulticastLock mcastLock;
	private WifiManager.WifiLock wifiLock;

	public ConnectivityBroadcastReceiver(AirTube at) {
		this.airtube = at;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		/* WIFI */
		if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
			NetworkInfo info = (NetworkInfo)intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			Log.d(TAG, "+++ WIFI " + String.valueOf(info));

			/* WIFI disconnected */
			if (info != null && info.getType() == ConnectivityManager.TYPE_WIFI && !info.isConnectedOrConnecting()) {
				wifiDisconnected();
			}
		}
		/* 3G or WIFI */
		else if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
			ConnectivityManager connectivityManager = ((ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE));
			NetworkInfo info = connectivityManager.getActiveNetworkInfo();
			Log.d(TAG, "+++ CONN " + String.valueOf(info));

			/* 3G or WIFI connected */
			if (info != null && info.isConnected()) {
				if (info.getType() == ConnectivityManager.TYPE_WIFI) {
					wifiConnected(context);
				} else {
					mobileConnected();
				}
			}
			/* 3G disconnected */
			else if (info == null || !info.isConnected()) {
				/* this event does not come when we change Wifi or change from 3G to Wifi networks,
				 * only when we disconnect from 3G and Wifi is not active, in which case we get null */
				mobileDisconnected();
			}
		}
	}

	private void mobileConnected() {
		if (connected3G)
			return;
		Log.d(TAG, "+++ connected 3G");
		connected3G = true;
		startAirTube();
	}

	private void mobileDisconnected() {
		if (!connected3G)
			return;
		Log.d(TAG, "+++ disconnected 3G");
		connected3G = false;
		stopAirTube();
	}

	private void wifiConnected(Context context) {
		if (connectedWIFI)
			return;
		Log.d(TAG, "+++ connected WIFI");
		connectedWIFI = true;

		/* aparently we need to re-acquire the multicast lock every time the connection changes */
		WifiManager wifi = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
		if (wifi != null) {
			wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF , "AirTube WifiLock"); //WIFI_MODE_FULL still looses packets
			wifiLock.acquire();
			Log.d(TAG, "--- acquired wifi lock: " + wifiLock.isHeld());

			mcastLock = wifi.createMulticastLock("AirTube MulticastLock");
			mcastLock.acquire();
			Log.d(TAG, "--- acquired multicast lock: " + mcastLock.isHeld());
		}

		if (connected3G) {
			/* need to re-start AirTube */
			connected3G = false;
			stopAirTube();
		}
		startAirTube();
	}

	private void wifiDisconnected() {
		if (!connectedWIFI)
			return;
		Log.d(TAG, "+++ disconnected wifi");
		connectedWIFI = false;

		if (mcastLock != null) {
			mcastLock.release();
			Log.d(TAG, "--- released multicast lock: " + !mcastLock.isHeld());
			mcastLock = null;
		}
		if (wifiLock != null) {
			wifiLock.release();
			Log.d(TAG, "--- released wifi lock: " + !wifiLock.isHeld());
			wifiLock = null;
		}
		stopAirTube();
	}

	private void startAirTube() {
		Log.d(TAG, "++++++ starting AirTube");
		airtube.start();
	}

	private void stopAirTube() {
		Log.d(TAG, "++++++ stopping AirTube");
		airtube.stop();
	}
}
