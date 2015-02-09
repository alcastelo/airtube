package com.thinktube.airtube.android;
import com.thinktube.airtube.android.ServiceDataParcel;
import com.thinktube.airtube.android.ServiceDescriptionParcel;
import com.thinktube.airtube.android.AirTubeIDParcel;
import com.thinktube.airtube.android.AirTubeCallbackAidl;
import com.thinktube.airtube.android.ConfigParametersParcel;

/* Due to the AIDL limitation that it can not subclass or implement interfaces,
 * this needs to be manually kept in sync with AirTubeInterfaceI.java */

interface AirTubeInterfaceAidl {
	/* service */
    AirTubeIDParcel registerService(in ServiceDescriptionParcel desc, in AirTubeCallbackAidl cb);
	boolean unregisterService(in AirTubeIDParcel serviceId);
	int getNumberOfClients(in AirTubeIDParcel serviceId);
	void sendServiceData(in AirTubeIDParcel serviceId, in ServiceDataParcel data);

	/* clients */
	AirTubeIDParcel registerClient(in AirTubeCallbackAidl client);
	boolean unregisterClient(in AirTubeIDParcel clientId);
	void findServices(in ServiceDescriptionParcel desc, in AirTubeIDParcel clientId);
	void unregisterServiceInterest(in ServiceDescriptionParcel desc, in AirTubeIDParcel clientId);
	void subscribeService(in AirTubeIDParcel serviceId, in AirTubeIDParcel clientId, in ConfigParametersParcel conf);
	void unsubscribeService(in AirTubeIDParcel serviceId, in AirTubeIDParcel clientId);
	void sendData(in AirTubeIDParcel serviceId, in AirTubeIDParcel clientId, in ServiceDataParcel data);

	/* any */
	ServiceDescriptionParcel getDescription(in AirTubeIDParcel serviceID);
}
