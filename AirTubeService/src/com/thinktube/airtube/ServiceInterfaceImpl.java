package com.thinktube.airtube;

import android.os.RemoteException;
import android.util.Log;

public class ServiceInterfaceImpl extends ServiceInterface.Stub {
	private static final String TAG = "AirTubeServiceServ";
	ServiceRegistry services;

	ServiceInterfaceImpl(ServiceRegistry services) {
		this.services = services;
	}

	@Override
	public int registerService(ServiceDescription serv) {
		Log.d(TAG, "register called");
		return services.addLocal(serv);
	}

	@Override
	public void unregisterService(int id) throws RemoteException {
		Log.d(TAG, "unregister called");
		services.removeLocal(id);
	}

	@Override
	public void sendData(int id, ServiceData data) throws RemoteException {
		Log.d(TAG, "send data: " + data.test);
		AService as = services.localServ.get(id);
		for (AClient ac : as.clients) {
			ac.receiveData(as, data);
		}
	}
}
