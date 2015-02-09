package com.thinktube.airtube.android;

import com.thinktube.airtube.android.ServiceDataParcel;
import com.thinktube.airtube.android.AirTubeIDParcel;
import com.thinktube.airtube.android.ServiceDescriptionParcel;
import com.thinktube.airtube.android.ConfigParametersParcel;

/* Due to the AIDL limitation that it can not subclass or implement interfaces,
 * this needs to be manually kept in sync with ClientCallbackI.java */

interface AirTubeCallbackAidl {
	void receiveData(in AirTubeIDParcel serviceId, in ServiceDataParcel data);

	void onSubscription(in AirTubeIDParcel serviceId, in AirTubeIDParcel clientId, in ConfigParametersParcel config);
	void onUnsubscription(in AirTubeIDParcel serviceId, in AirTubeIDParcel clientId);

	void onServiceFound(in AirTubeIDParcel serviceId, in ServiceDescriptionParcel desc);
}
