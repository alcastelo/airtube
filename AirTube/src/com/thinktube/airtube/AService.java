package com.thinktube.airtube;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public abstract class AService {
	private final static Logger LOG = Logger.getLogger(AService.class.getSimpleName());
	public AirTubeID id;
	public ServiceDescription desc;
	protected ConcurrentHashMap<AirTubeID, AClient> clients;
	protected DataTransmit sender;

	AService(AirTubeID id, ServiceDescription desc) {
		this.desc = desc;
		this.id = id;
		this.clients = new ConcurrentHashMap<AirTubeID, AClient>();
		this.sender = DataTransmit.getInstance();
	}

	@Override
	public String toString() {
		return "AService [" + id + "|" + desc.type + "]";
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
		AService other = (AService) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	public AClient getClient(AirTubeID id) {
		return clients.get(id);
	}

	public Collection<AClient> getClients() {
		return clients.values();
	}

	private void addSubscription(AClient ac) {
		if (clients.put(ac.id, ac) == null) {
			notifySubscription(ac);
		}
	}

	public void removeSubscription(AClient ac) {
		if (clients.remove(ac.id) != null) {
			notifyUnsubscription(ac);
		}
	}

	public void removeAllClientSubscriptions() {
		/* this just removes the subscriptions, calls callbacks
		 * but does not send UNSUBSCRIBE messages */
		for (AClient ac : clients.values()) {
			this.removeSubscription(ac);
			ac.removeSubscription(this);
		}
	}

	public void unsubscribeAllClients() {
		/* this may send UNSUBSCRIBE messages over the network */
		for (AClient ac : clients.values()) {
			this.unsubscribe(ac);
		}
	}

	protected void notifySubscription(AClient as) {
		AirTube.getMonitor().addSubscription(this.id, as.id);
	};

	protected void notifyUnsubscription(AClient as) {
		AirTube.getMonitor().removeSubscription(this.id, as.id);
	};

	public void notifyAllSubscriptions(MonitorCallbackI mon) {
		for (AirTubeID cid : clients.keySet()) {
			mon.addSubscription(this.id, cid);
		}
	}

	public void subscribe(AClient ac) {
		LOG.info("subscribe " + ac + " to " + this);
		this.addSubscription(ac);
		ac.addSubscription(this);
	}

	public void unsubscribe(AClient ac) {
		LOG.info("unsubscribe " + ac + " from " + this);
		this.removeSubscription(ac);
		ac.removeSubscription(this);
	}

	abstract public void receiveData(AirTubeID clientId, ServiceData data);
}
