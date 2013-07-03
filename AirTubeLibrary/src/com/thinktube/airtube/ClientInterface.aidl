package com.thinktube.airtube;
import com.thinktube.airtube.ServiceData;
import com.thinktube.airtube.ClientCallback;

interface ClientInterface {
	int subscribeService(in String name, in ClientCallback client);
	void unsubscribeService(in String name, int id);
}
