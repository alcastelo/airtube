package com.thinktube.airtube.android;

import java.math.BigInteger;

import com.thinktube.airtube.AirTube;
import com.thinktube.airtube.android.AirTubeInterfaceAidl;
import com.thinktube.airtube.android.ConnectivityBroadcastReceiver;
import com.thinktube.airtube.android.gui.AirTubeMonitorActivity;

import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings.Secure;

public class AirTubeService extends Service {
	private static final String TAG = "AirTubeService";
	private AirTube airTube;
	private AirTubeInterfaceAidl.Stub serverBinder;
	private MonitorMessenger monitor;
	private ConnectivityBroadcastReceiver wifiWatcher;
	private WakeLock wakeLock;

	@Override
	public void onCreate() {
		Log.d(TAG, "created");

		String androidId = Secure.getString(getBaseContext().getContentResolver(), Secure.ANDROID_ID);
		/* parse thru BigInteger because androidId really is a unsigned long value
		 * and could be too large for Long.parseLong(androidId, 16) */
		long deviceId = new BigInteger(androidId, 16).longValue();

		airTube = new AirTube(deviceId);

		serverBinder = new AirTubeInterfaceImpl(airTube);
		monitor = new MonitorMessenger(airTube);

		/* add notification to see when AirTube is running and to start the GUI */
		Intent guiIntent = new Intent(this, AirTubeMonitorActivity.class);
		guiIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, guiIntent, PendingIntent.FLAG_ONE_SHOT);

		Notification noti = new NotificationCompat.Builder(this)
				.setContentTitle("AirTube service")
				.setContentText("Running...")
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentIntent(pIntent)
				.build();

		/* we start in the foreground because AirTube should always keep on running */
		startForeground(1, noti);

		wifiWatcher = new ConnectivityBroadcastReceiver(airTube);
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(wifiWatcher, intentFilter);

		/* should avoid this if possible to save battery life,
		 * but without it timers and thread.sleep() get very inexact
		 *
		 * TODO: implement smarter power save mode in AirTube */
		PowerManager lPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = lPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AirTubeWakeLock");
		wakeLock.acquire();
		Log.i(TAG, "--- acquired wake lock: " + wakeLock.isHeld());
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "start command");
		return START_STICKY; // restart the service if it got killed
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "bind " + intent.getAction());
		if (intent.getAction().equals("AirTube_server"))
			return serverBinder;
		else if (intent.getAction().equals("AirTube_monitor"))
			return monitor.getBinder();
		else
			return null;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.d(TAG, "unbind " + intent.getAction());
		/* returning true here would make onRebind() be called instead of onBind()
		 * when the next client connects. */
		return false;
	}

	@Override
	public void onRebind(Intent intent) {
		// should not be called because onUnbind() returns false
		Log.d(TAG, "rebind " + intent.getAction());
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "destroyed");
		airTube.stop();
		stopForeground(true);
		unregisterReceiver(wifiWatcher);
		wakeLock.release();
		this.stopSelf();
	}
}
