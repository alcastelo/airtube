package com.thinktube.airtube;

import java.util.concurrent.CopyOnWriteArraySet;

public abstract class AClient {
	private CopyOnWriteArraySet <AService> services;
	AirTubeID id;
	ConfigParameters config;

	AClient(AirTubeID id) {
		this(id, new ConfigParameters());
	}

	AClient(AirTubeID id, ConfigParameters conf) {
		this.id = id;
		this.config = conf;
		this.services = new CopyOnWriteArraySet<AService>();
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AClient other = (AClient) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	public void addSubscription(AService as) {
		if (services.add(as)) {
			notifySubscription(as);
		}
	}

	public void removeSubscription(AService as) {
		if (services.remove(as)) {
			notifyUnsubscription(as);
		}
	}

	public void removeAllServiceSubscriptions() {
		for (AService as : services) {
			as.removeSubscription(this);
			this.removeSubscription(as);
		}
	}

	public void unsubscribeFromAllServices() {
		for (AService as : services) {
			as.unsubscribe(this);
		}
	}

	public boolean isSubscribed() {
		return !services.isEmpty();
	}

	protected void notifySubscription(AService as) { };
	protected void notifyUnsubscription(AService as) { };

	public abstract void receiveData(AirTubeID from, ServiceData data, TransmissionType type, TrafficClass tos);
}
