package com.thinktube.airtube;
import com.thinktube.airtube.ServiceData;
import com.thinktube.airtube.ServiceDescription;

interface ServiceInterface {
    int registerService(in ServiceDescription service);
	void unregisterService(int id);
	void sendData(int id, in ServiceData data);
}
