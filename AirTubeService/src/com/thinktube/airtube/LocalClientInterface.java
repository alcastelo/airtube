package com.thinktube.airtube;

import java.util.List;

import android.os.RemoteException;
import android.util.Log;

public class LocalClientInterface extends ClientInterface.Stub {
	private static final String TAG = null;
	ServiceRegistry services;
	private SubscriptionManager sm;

	LocalClientInterface(ServiceRegistry services, SubscriptionManager sm) {
		this.services = services;
		this.sm = sm;
	}

	@Override
	public int subscribeService(String name, ClientCallback client)
			throws RemoteException {
		Log.d(TAG, "subscribe client");

		LocalClient lc = new LocalClient(client);
		List<AService> list = services.findAllServices(name);
		
		for (AService as : list) {
			as.clients.add(lc);

			for (Peer p : as.providers)
				sm.subscribe(name, p.ip);
		}

		return 0; //TODO
	}

	@Override
	public void unsubscribeService(String name, int id) throws RemoteException {
		Log.d(TAG, "unsubscribe client");
		services.removeClient(name, id);
	}
}
