package com.thinktube.airtube;

import android.os.RemoteException;
import android.util.Log;

public class LocalClient extends AClient {
	ClientCallback cb;

	LocalClient(ClientCallback cb) {
		super();
		this.cb = cb;
	}

	@Override
	public void receiveData(AService as, ServiceData data) {
		Log.d("LocalClient", "send data via callback");
		try {
			cb.receiveData(data);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
