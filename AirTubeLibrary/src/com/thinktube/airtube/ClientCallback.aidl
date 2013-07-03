package com.thinktube.airtube;

import com.thinktube.airtube.ServiceData;

interface ClientCallback {
	void receiveData(in ServiceData data);
}
