package com.thinktube.airtube;

public abstract class AClient {
	AClient() {
	}

	public abstract void receiveData(AService serv, ServiceData data);
}
