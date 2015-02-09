package com.thinktube.airtube;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

public class AirTubeRegistry {
	private final static Logger LOG = Logger.getLogger(AirTubeRegistry.class.getSimpleName());
	private ConcurrentHashMap<AirTubeID, LocalService> localServ = new ConcurrentHashMap<AirTubeID, LocalService>();
	private ConcurrentHashMap<ServiceDescription, AirTubeID> oldLocalServIDs = new ConcurrentHashMap<ServiceDescription, AirTubeID>();
	private ConcurrentHashMap<AirTubeID, LocalClient> localClients = new ConcurrentHashMap<AirTubeID, LocalClient>();
	private ConcurrentHashMap<Long, Peer> peers = new ConcurrentHashMap<Long, Peer>();
	private ConcurrentHashMap<ServiceDescription, Set<LocalClient>> serviceInterest = new ConcurrentHashMap<ServiceDescription, Set<LocalClient>>();
	private short lastAirTubeID = 1;
	public long deviceId;
	private static AirTubeRegistry instance;

	public AirTubeRegistry(long deviceId) {
		this.deviceId = deviceId;
		AirTubeRegistry.instance = this;
	}

	public static AirTubeRegistry getInstance() {
		return instance;
	}

	public LocalService addLocalService(ServiceDescription desc, AirTubeCallbackI cb) {
		AirTubeID id = oldLocalServIDs.remove(desc);
		if (id != null) {
			LOG.info("reusing existing serviceID " + id);
		} else {
			id = new AirTubeID(lastAirTubeID++, deviceId);
		}
		LocalService ls = new LocalService(id, desc, cb);
		if (localServ.put(ls.id, ls) != null) {
			LOG.warning("new " + ls + " already registered?");
		} else {
			LOG.fine("added " + ls);
			serviceAddedCallback(ls);
		}
		return ls;
	}

	public LocalService removeLocalService(AirTubeID id) {
		LOG.fine("remove local service " + id);
		LocalService ls = localServ.remove(id);
		if (ls == null) {
			LOG.warning("local service " + id + " not known?");
		} else {
			AirTube.getMonitor().removeService(id);
			oldLocalServIDs.put(ls.desc, ls.id);
		}
		return ls;
	}

	public LocalService getLocalService(AirTubeID id) {
		return localServ.get(id);
	}

	public LocalService getLocalService(short sid) {
		AirTubeID id = new AirTubeID(sid, deviceId);
		return localServ.get(id);
	}

	public Collection<LocalService> getLocalServices() {
		return localServ.values();
	}

	/* peers */

	public void addPeer(Peer p) {
		if (peers.put(p.deviceId, p) != null) {
			throw new RuntimeException(p + " already registered!");
		} else {
			LOG.info(p + " registered");
			AirTube.getMonitor().addPeer(p.deviceId);
		}
	}

	public Peer getPeer(long deviceId) {
		return peers.get(deviceId);
	}

	public Collection<Peer> getAllPeers() {
		return peers.values();
	}

	public void removePeer(long deviceId) {
		if (peers.remove(deviceId) != null) {
			LOG.info("Peer [" + Long.toHexString(deviceId) + "] removed");
			AirTube.getMonitor().removePeer(deviceId);
		} else {
			throw new RuntimeException("Peer [" + Long.toHexString(deviceId) + "] is not registered!");
		}
	}

	public List<AirTubeID> findAllServices(ServiceDescription desc) {
		List<AirTubeID> list = new ArrayList<AirTubeID>();
		for (AService as : localServ.values()) {
			if (as.desc.equals(desc)) {
				LOG.fine("found local " + as.id);
				list.add(as.id);
			}
		}
		for (Peer p : peers.values()) {
			for (AService as : p.getServices()) {
				if (as.desc.equals(desc)) {
					LOG.fine("found remote " + as.id);
					list.add(as.id);
				}
			}
		}
		return list;
	}

	public List<ServiceDescription> getLocalServiceDescriptions() {
		List<ServiceDescription> list = new ArrayList<ServiceDescription>();
		for (LocalService as : localServ.values()) {
			list.add(as.desc);
		}
		return list;
	}

	public AService getService(AirTubeID id) {
		AService as = getLocalService(id);
		if (as == null) {
			Peer p = peers.get(id.deviceId);
			as = p.getService(id.port);
		}
		return as;
	}

	// local clients
	public LocalClient addLocalClient(AirTubeCallbackI cb) {
		AirTubeID id = new AirTubeID(lastAirTubeID++, deviceId);
		LocalClient lc = new LocalClient(id, cb);
		if (localClients.put(id, lc) != null) {
			LOG.warning("new " + lc + " already registered?");
		} else {
			AirTube.getMonitor().addClient(id, MonitorCallbackI.Location.LOCAL);
			LOG.fine("added " + lc);
		}
		return lc;
	}

	public LocalClient removeLocalClient(AirTubeID id) {
		LOG.fine("remove local client " + id);
		LocalClient lc = localClients.remove(id);
		if (lc == null) {
			LOG.warning("local client " + id + " not known?");
		} else {
			AirTube.getMonitor().removeClient(id);
		}
		return lc;
	}

	public LocalClient getLocalClient(AirTubeID id) {
		return localClients.get(id);
	}

	public Collection<LocalClient> getLocalClients() {
		return localClients.values();
	}

	public synchronized void registerServiceInterest(ServiceDescription desc, LocalClient lc) {
		/* create set as necessary */
		Set<LocalClient> cls = serviceInterest.get(desc);
		if (cls == null) {
			cls = new CopyOnWriteArraySet<LocalClient>();
			cls.add(lc);
			serviceInterest.put(desc, cls);
		} else {
			cls.add(lc);
		}

		/* call callbacks for known services */
		for (AService as : localServ.values()) {
			if (as.desc.equals(desc)) {
				lc.notifyServiceFound(as);
			}
		}
		for (Peer p : peers.values()) {
			for (AService as : p.getServices()) {
				if (as.desc.equals(desc)) {
					lc.notifyServiceFound(as);
				}
			}
		}
	}

	public void unregisterServiceInterest(ServiceDescription desc, LocalClient lc) {
		LOG.fine("unregister ServiceInterest for: " + desc.name + " of " + lc);

		Set<LocalClient> cls = serviceInterest.get(desc);
		if (cls != null) {
			if (!cls.remove(lc)) {
				LOG.fine("ServiceInterest not removed");
			}
			if (cls.isEmpty()) {
				serviceInterest.remove(desc);
			}
		} else {
			LOG.fine("ServiceInterest description not found: " + desc.name);
		}
	}

	public void unregisterServiceInterest(LocalClient lc) {
		LOG.fine("unregister all ServiceInterests for " + lc);
		for (Entry<ServiceDescription, Set<LocalClient>> e : serviceInterest.entrySet()) {
			if (e.getValue().remove(lc)) {
				LOG.fine("removed serviceInterest " + lc + " from " + e.getKey());
				if (e.getValue().isEmpty()) {
					serviceInterest.remove(e.getKey());
					LOG.fine("removed empty serviceInterest " + e.getKey());
				}
			}
		}
	}

	public void serviceAddedCallback(AService as) {
		AirTube.getMonitor().addService(as.id, as.desc, as instanceof LocalService ?
				MonitorCallbackI.Location.LOCAL : MonitorCallbackI.Location.REMOTE);

		/* find interested clients */
		for (Entry<ServiceDescription, Set<LocalClient>> e : serviceInterest.entrySet()) {
			if (e.getKey().equals(as.desc)) {
				for (LocalClient lc : e.getValue()) {
					lc.notifyServiceFound(as);
				}
			}
		}
	}

	public void clearAll() {
		localServ.clear();
		oldLocalServIDs.clear();
		localClients.clear();
		serviceInterest.clear();
		peers.clear();
		lastAirTubeID = 1;
	}
}
