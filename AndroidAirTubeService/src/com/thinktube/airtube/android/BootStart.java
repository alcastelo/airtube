package com.thinktube.airtube.android;

import android.util.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootStart extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("BootStart", "starting AirTube");
		Intent serviceIntent = new Intent("AirTube_server");
		context.startService(serviceIntent);
	}
}