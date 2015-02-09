package com.thinktube.airtube.android;

import com.thinktube.airtube.AirTubeID;
import com.thinktube.airtube.android.AirTubeCallbackAidl;
import com.thinktube.airtube.android.AirTubeInterfaceAidl;
import com.thinktube.airtube.AirTubeInterfaceI;

public class AirTubeInterfaceImpl extends AirTubeInterfaceAidl.Stub {
	private final AirTubeInterfaceI airTube;

	AirTubeInterfaceImpl(AirTubeInterfaceI airTube) {
		this.airTube = airTube;
	}

	@Override
	public AirTubeIDParcel registerService(ServiceDescriptionParcel desc, AirTubeCallbackAidl cb) {
		AirTubeCallbackWrapper scw = new AirTubeCallbackWrapper(cb, airTube);
		AirTubeID sid = airTube.registerService(desc, scw);
		AirTubeIDParcel asid = new AirTubeIDParcel(sid);
		scw.setID(sid);
		return asid;
	}

	@Override
	public boolean unregisterService(AirTubeIDParcel sid) {
		return airTube.unregisterService(sid);
	}

	@Override
	public void sendServiceData(AirTubeIDParcel sid, ServiceDataParcel data) {
		airTube.sendServiceData(sid, data);
	}

	@Override
	public int getNumberOfClients(AirTubeIDParcel sid) {
		return airTube.getNumberOfClients(sid);
	}
	
	/* clients */

	@Override
	public AirTubeIDParcel registerClient(AirTubeCallbackAidl cb) {
		AirTubeCallbackWrapper cbw = new AirTubeCallbackWrapper(cb, airTube);
		AirTubeID cid = airTube.registerClient(cbw);
		AirTubeIDParcel acid = new AirTubeIDParcel(cid);
		cbw.setID(cid);
		return acid;
	}

	@Override
	public boolean unregisterClient(AirTubeIDParcel cid) {
		return airTube.unregisterClient(cid);
	}

	@Override
	public void subscribeService(AirTubeIDParcel sid, AirTubeIDParcel cid, ConfigParametersParcel conf) {
		airTube.subscribeService(sid, cid, conf);
	}

	@Override
	public void unsubscribeService(AirTubeIDParcel sid, AirTubeIDParcel cid) {
		airTube.unsubscribeService(sid, cid);
	}

	@Override
	public void unregisterServiceInterest(ServiceDescriptionParcel desc, AirTubeIDParcel cid) {
		airTube.unregisterServiceInterest(desc, cid);
	}

	@Override
	public void findServices(ServiceDescriptionParcel desc, AirTubeIDParcel cid) {
		airTube.findServices(desc, cid);
	}

	@Override
	public void sendData(AirTubeIDParcel sid, AirTubeIDParcel cid, ServiceDataParcel data) {
		airTube.sendData(sid, cid, data);
	}

	/* any */

	@Override
	public ServiceDescriptionParcel getDescription(AirTubeIDParcel sid) {
		return new ServiceDescriptionParcel(airTube.getDescription(sid));
	}
}
