package com.thinktube.airtube;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class Peer {
	private final static Logger LOG = Logger.getLogger(Peer.class.getSimpleName());
	public long deviceId;
	private ConcurrentHashMap<Short, RemoteService> services = new ConcurrentHashMap<Short, RemoteService>();
	private ConcurrentHashMap<AirTubeID, RemoteClient> clients = new ConcurrentHashMap<AirTubeID, RemoteClient>();
	private AirTubeRegistry registry;

	public Peer(long deviceId, AirTubeRegistry r) {
		this.deviceId = deviceId;
		this.registry = r;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (deviceId ^ (deviceId >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Peer other = (Peer) obj;
		if (deviceId != other.deviceId)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Peer [" + Long.toHexString(deviceId) + "]";
	}

	/* services */

	public void addService(RemoteService rs) {
		if (services.put(rs.id.port, rs) != null) {
			LOG.warning("new " + rs + " already registered?");
		} else {
			registry.serviceAddedCallback(rs);
		}
	}

	public RemoteService getService(short port) {
		return services.get(port);
	}

	public Collection<RemoteService> getServices() {
		return services.values();
	}

	public void removeService(RemoteService rs) {
		if (services.remove(rs.id.port) == null) {
			LOG.warning("remote service " + rs + " not known?");
		} else {
			SubscriptionManager.getInstance().removePendingSubscriptionsForService(rs.id);
			AirTube.getMonitor().removeService(rs.id);
		}
	}

	public void removeAllServices() {
		for (RemoteService rs: services.values()) {
			LOG.fine("remove " + rs);
			rs.removeAllClientSubscriptions();
			if (services.remove(rs.id.port) == null) {
				LOG.warning("remote service " + rs + " not known?");
			}
			SubscriptionManager.getInstance().removePendingSubscriptionsForService(rs.id);
			AirTube.getMonitor().removeService(rs.id);
		}
	}

	/* clients */

	public void addClient(RemoteClient rc) {
		if (clients.put(rc.id, rc) != null) {
			LOG.warning("new " + rc + " already registered?");
		} else {
			AirTube.getMonitor().addClient(rc.id, MonitorCallbackI.Location.REMOTE);
			LOG.fine("added " + rc);
		}
	}

	public RemoteClient getOrAddClient(AirTubeID id, ConfigParameters conf) {
		RemoteClient rc = clients.get(id);
		if (rc != null) {
			return rc;
		} else {
			rc = new RemoteClient(id, this, conf);
			addClient(rc);
		}
		return rc;
	}

	public Collection<RemoteClient> getClients() {
		return clients.values();
	}

	public void removeClient(RemoteClient rc) {
		if (clients.remove(rc.id) == null) {
			LOG.warning("remote client " + rc + " not known?");
		} else {
			AirTube.getMonitor().removeClient(rc.id);
		}
	}

	public void removeAllClients() {
		for (RemoteClient rc : clients.values()) {
			LOG.fine("remove " + rc);
			rc.removeAllServiceSubscriptions();
			clients.remove(rc.id);
		}
	}

	public void timeout() {
		removeAllServices();
		removeAllClients();
	}
}
