package com.thinktube.airtube;

import com.thinktube.airtube.ServiceInterface;
import com.thinktube.airtube.ClientInterface;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class AirTubeService extends Service {
	private static final String TAG = "AirTubeService";

	ServiceRegistry services = new ServiceRegistry();
	Peers peers = new Peers();

	ServiceAnnounce sa = new ServiceAnnounce(services, peers);
	DataTransmit dt = new DataTransmit(services);
	SubscriptionManager sm = new SubscriptionManager(services, peers, dt);

	NotificationManager notificationManager;

	private final ServiceInterface.Stub serverBinder = new ServiceInterfaceImpl(services);
	private final ClientInterface.Stub clientBinder = new LocalClientInterface(services, sm);

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "bind " + intent.getAction());
		if (intent.getAction().equals("AirTube_server"))
			return serverBinder;
		else if (intent.getAction().equals("AirTube_client"))
			return clientBinder;
		else
			return null;
	}

	@Override
	public void onCreate() {
		Log.d(TAG, "created");

		// Prepare intent which is triggered if the notification is selected
		Intent intent1 = new Intent(this, ServiceControlActivity.class);
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent1, 0);

		Notification noti = new Notification.Builder(this)
				.setContentTitle("AirTube service framework")
				.setContentText("Running...")
				.setSmallIcon(R.drawable.ic_launcher).setContentIntent(pIntent)
				.build();

		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.notify(0, noti);

		sa.start();
		sm.start();
		dt.start();
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "destroyed");
		sa.stop();
		sm.stop();
		dt.stop();
		notificationManager.cancel(0);
		this.stopSelf();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "start command");
		return START_STICKY;
	}
}