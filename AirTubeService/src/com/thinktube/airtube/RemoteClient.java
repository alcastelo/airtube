package com.thinktube.airtube;

import android.util.Log;

public class RemoteClient extends AClient {
	Peer peer;
	DataTransmit dt;

	RemoteClient(Peer p, DataTransmit dt) {
		super();
		this.peer = p;
		this.dt = dt;
	}

	@Override
	public void receiveData(AService as, ServiceData data) {
		Log.d("RemoteClient", "send data to " + peer.ip.toString());
		dt.sendUDP(peer.ip, as, data);
	}
}
