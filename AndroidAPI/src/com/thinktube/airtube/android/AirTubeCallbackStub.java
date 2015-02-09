package com.thinktube.airtube.android;

import android.os.Handler;
import android.os.RemoteException;

import com.thinktube.airtube.android.AirTubeIDParcel;
import com.thinktube.airtube.AirTubeCallbackI;

/* only used by AirTubeServiceConnection */

public class AirTubeCallbackStub extends AirTubeCallbackAidl.Stub {
	private final AirTubeCallbackI cb;
	private final Handler handler = new Handler();

	public AirTubeCallbackStub(AirTubeCallbackI cb) {
		this.cb = cb;
	}

	@Override
	public void receiveData(final AirTubeIDParcel fromId, final ServiceDataParcel data) throws RemoteException {
		handler.post(new Runnable() {
			public void run() {
				cb.receiveData(fromId, data);
			}
		});
	}

	@Override
	public void onSubscription(final AirTubeIDParcel serviceId, final AirTubeIDParcel clientId, final ConfigParametersParcel config) {
		handler.post(new Runnable() {
			public void run() {
				cb.onSubscription(serviceId, clientId, config);
			}
		});
	}

	@Override
	public void onUnsubscription(final AirTubeIDParcel serviceId, final AirTubeIDParcel clientId) {
		handler.post(new Runnable() {
			public void run() {
				cb.onUnsubscription(serviceId, clientId);
			}
		});
	}

	@Override
	public void onServiceFound(final AirTubeIDParcel serviceId, final ServiceDescriptionParcel desc) {
		handler.post(new Runnable() {
			public void run() {
				cb.onServiceFound(serviceId, desc);
			}
		});
	}
}
