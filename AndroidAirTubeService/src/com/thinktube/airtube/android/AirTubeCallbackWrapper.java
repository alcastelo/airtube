package com.thinktube.airtube.android;

import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.RemoteException;

import com.thinktube.airtube.AirTubeID;
import com.thinktube.airtube.AirTubeCallbackI;
import com.thinktube.airtube.AirTubeInterfaceI;
import com.thinktube.airtube.ConfigParameters;
import com.thinktube.airtube.ServiceData;
import com.thinktube.airtube.ServiceDescription;
import android.util.Log;

/* only used by AirTubeInterfaceImpl */

public class AirTubeCallbackWrapper implements AirTubeCallbackI {
	protected static final String TAG = "AirTubeCallbackWrapper";
	private AirTubeCallbackAidl cb;
	private final AirTubeInterfaceI airtube;
	private AirTubeID id;

	private final IBinder.DeathRecipient tod = new IBinder.DeathRecipient() {
		@Override
		public void binderDied() {
			Log.d(TAG, "Binder died, unregistering " + id);
			unregister();
		}
	};

	AirTubeCallbackWrapper(AirTubeCallbackAidl cb, AirTubeInterfaceI air) {
		this.cb = cb;
		this.airtube = air;
		try {
			this.cb.asBinder().linkToDeath(tod, 0);
		} catch (RemoteException e) {
			// binder already dead
			this.cb = null;
		}
	}

	@Override
	public void receiveData(AirTubeID serviceId, ServiceData data) {
		if (cb == null)
			return;

		try {
			cb.receiveData(new AirTubeIDParcel(serviceId), new ServiceDataParcel(data));
		} catch (DeadObjectException de) {
			handleDeadObjectEx(de);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onSubscription(AirTubeID serviceId, AirTubeID clientId, ConfigParameters config) {
		if (cb == null)
			return;

		try {
			cb.onSubscription(new AirTubeIDParcel(serviceId), new AirTubeIDParcel(clientId), new ConfigParametersParcel(config));
		} catch (DeadObjectException de) {
			handleDeadObjectEx(de);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onUnsubscription(AirTubeID serviceId, AirTubeID clientId) {
		if (cb == null)
			return;

		try {
			cb.onUnsubscription(new AirTubeIDParcel(serviceId), new AirTubeIDParcel(clientId));
		} catch (DeadObjectException de) {
			handleDeadObjectEx(de);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onServiceFound(AirTubeID serviceId, ServiceDescription desc) {
		if (cb == null)
			return;

		try {
			cb.onServiceFound(new AirTubeIDParcel(serviceId), new ServiceDescriptionParcel(desc));
		} catch (DeadObjectException de) {
			handleDeadObjectEx(de);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void setID(AirTubeID id) {
		this.id = id;
	}

	private void unregister() {
		cb = null;
		if (id != null) {
			// TODO: unify
			airtube.unregisterService(id);
			airtube.unregisterClient(id);
		}
		else
			Log.e(TAG, "id is null?");
	}

	private void handleDeadObjectEx(DeadObjectException de) {
		Log.e(TAG, "DeadObjectException, unregistering " + id);
		unregister();
	}
}
