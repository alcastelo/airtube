package com.thinktube.airtube.android;

import java.util.HashSet;
import java.util.Set;

import com.thinktube.airtube.AirTubeID;
import com.thinktube.airtube.android.AirTubeInterfaceAidl;
import com.thinktube.airtube.AirTubeCallbackI;
import com.thinktube.airtube.ConfigParameters;
import com.thinktube.airtube.AirTubeConnectionCallbackI;
import com.thinktube.airtube.ServiceData;
import com.thinktube.airtube.ServiceDescription;
import com.thinktube.airtube.AirTubeInterfaceI;
import android.util.Log;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

public class AirTubeServiceConnection implements AirTubeInterfaceI {
	protected static final String TAG = "AirTubeServiceInterface";
	Intent intent = new Intent("AirTube_server");
	private boolean bound = false;
	private Context context;
	private AirTubeInterfaceAidl serviceIf;
	private Set<AirTubeConnectionCallbackI> cbs = new HashSet<AirTubeConnectionCallbackI>();

	private ServiceConnection serviceConn = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.d(TAG, "connected to AirTube service");
			serviceIf = AirTubeInterfaceAidl.Stub.asInterface(service);
			for (AirTubeConnectionCallbackI cb : cbs) {
				cb.onConnect(AirTubeServiceConnection.this);
			}
		}
		public void onServiceDisconnected(ComponentName className) {
			Log.d(TAG, "AirTube service has disconnected");
			serviceIf = null;
			for (AirTubeConnectionCallbackI cb : cbs) {
				cb.onDisconnect();
			}
		}
	};

	public AirTubeServiceConnection(Context context, AirTubeConnectionCallbackI cb) {
		this.context = context;
		cbs.add(cb);
	}

	public AirTubeServiceConnection(Context context) {
		this.context = context;
	}

	@Override
	public void addComponent(AirTubeConnectionCallbackI cb) {
		cbs.add(cb);
	}

	@Override
	public void removeComponent(AirTubeConnectionCallbackI cb) {
		cbs.remove(cb);
	}

	public boolean bind() {
		if (bound) {
			Log.e(TAG, "already bound");
			return true;
		}

		if (context.startService(intent) != null) {
			Log.d(TAG, "Airtube service started");
		} else {
			Log.d(TAG, "Airtube service does not exist");
			return false;
		}

		if (context.bindService(intent, serviceConn, 0)) {
			Log.d(TAG, "Airtube service bound");
		} else {
			Log.d(TAG, "Airtube service does not exist");
			return false;
		}

		Log.d(TAG, "bound");
		bound = true;
		return true;
	}

	public void unbind() {
		if (!bound) {
			Log.e(TAG, "not bound");
			return;
		}

		try {
			context.unbindService(serviceConn);
		} catch (IllegalArgumentException e) {
			Log.d(TAG, "service no longer exists? " + e.toString());
		}
		Log.d(TAG, "unbound");
		bound = false;
	}

	@Override
	public AirTubeID registerService(ServiceDescription desc, AirTubeCallbackI cb) {
		if (!bound || serviceIf == null) {
			Log.e(TAG, "registerService: not connected");
			return null;
		}

		AirTubeCallbackStub scw = new AirTubeCallbackStub(cb);

		try {
			return serviceIf.registerService(ServiceDescriptionParcel.wrap(desc), scw);
		} catch (RemoteException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public boolean unregisterService(AirTubeID serviceId) {
		if (!bound || serviceIf == null) {
			Log.e(TAG, "unregisterService: not connected");
			return false;
		}

		try {
			return serviceIf.unregisterService(AirTubeIDParcel.wrap(serviceId));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public void sendServiceData(AirTubeID serviceId, ServiceData data) {
		if (!bound || serviceIf == null) {
			Log.e(TAG, "sendData: not connected");
			return;
		}

		try {
			serviceIf.sendServiceData(AirTubeIDParcel.wrap(serviceId), ServiceDataParcel.wrap(data));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public int getNumberOfClients(AirTubeID serviceId) {
		if (!bound || serviceIf == null) {
			Log.e(TAG, "getNumberOfClients: not connected");
			return -1;
		}

		try {
			return serviceIf.getNumberOfClients(AirTubeIDParcel.wrap(serviceId));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	/* clients */

	@Override
	public AirTubeID registerClient(AirTubeCallbackI client) {
		if (!bound || serviceIf == null) {
			Log.e(TAG, "registerClient: not connected");
			return null;
		}

		AirTubeCallbackStub cb = new AirTubeCallbackStub(client);
		try {
			return serviceIf.registerClient(cb);
		} catch (RemoteException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public boolean unregisterClient(AirTubeID clientId) {
		if (!bound || serviceIf == null) {
			Log.e(TAG, "unregisterClient: not connected");
			return false;
		}
		try {
			return serviceIf.unregisterClient(AirTubeIDParcel.wrap(clientId));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public void findServices(ServiceDescription desc, AirTubeID clientId) {
		if (!bound || serviceIf == null) {
			Log.e(TAG, "findServices: not connected");
			return;
		}
		try {
			serviceIf.findServices(ServiceDescriptionParcel.wrap(desc), AirTubeIDParcel.wrap(clientId));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void unregisterServiceInterest(ServiceDescription desc, AirTubeID clientId) {
		if (!bound || serviceIf == null) {
			Log.e(TAG, "unregisterServiceInterest: not connected");
			return;
		}
		try {
			serviceIf.unregisterServiceInterest(ServiceDescriptionParcel.wrap(desc), AirTubeIDParcel.wrap(clientId));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void subscribeService(AirTubeID serviceId, AirTubeID clientId, ConfigParameters conf) {
		if (!bound || serviceIf == null) {
			Log.e(TAG, "subscribeService: not connected");
			return;
		}
		try {
			serviceIf.subscribeService(AirTubeIDParcel.wrap(serviceId), AirTubeIDParcel.wrap(clientId), ConfigParametersParcel.wrap(conf));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void unsubscribeService(AirTubeID serviceId, AirTubeID clientId) {
		if (!bound || serviceIf == null) {
			Log.e(TAG, "unsubscribeService: not connected");
			return;
		}
		try {
			serviceIf.unsubscribeService(AirTubeIDParcel.wrap(serviceId), AirTubeIDParcel.wrap(clientId));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void sendData(AirTubeID serviceId, AirTubeID clientId, ServiceData data) {
		if (!bound || serviceIf == null) {
			Log.e(TAG, "sendData: not connected");
			return;
		}
		try {
			serviceIf.sendData(AirTubeIDParcel.wrap(serviceId), AirTubeIDParcel.wrap(clientId), ServiceDataParcel.wrap(data));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public ServiceDescription getDescription(AirTubeID serviceId) {
		if (!bound || serviceIf == null) {
			Log.e(TAG, "getDescription: not connected");
			return null;
		}
		try {
			return serviceIf.getDescription(AirTubeIDParcel.wrap(serviceId));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return null;
	}
}
