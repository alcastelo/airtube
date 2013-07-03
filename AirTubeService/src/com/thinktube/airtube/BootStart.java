package com.thinktube.airtube;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootStart extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("BBB", "boot start");
		Intent serviceIntent = new Intent("AirTube_server");
		context.startService(serviceIntent);
	}
}