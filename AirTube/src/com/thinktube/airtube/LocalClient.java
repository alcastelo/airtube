package com.thinktube.airtube;

public class LocalClient extends AClient {
	private AirTubeCallbackI cb;

	LocalClient(AirTubeID id, AirTubeCallbackI cb) {
		super(id);
		this.cb = cb;
	}

	@Override
	public void receiveData(AirTubeID from, ServiceData data, TransmissionType type, TrafficClass tos) {
		cb.receiveData(from, data);
	}

	@Override
	protected void notifySubscription(AService as) {
		cb.onSubscription(as.id, this.id, as.desc.config);
	}

	protected void notifyUnsubscription(AService as) {
		cb.onUnsubscription(as.id, this.id);
	}
	
	public void notifyServiceFound(AService as) {
		cb.onServiceFound(as.id, as.desc);
	}

	@Override
	public String toString() {
		return "LocalClient [" + id + "]";
	}
}
