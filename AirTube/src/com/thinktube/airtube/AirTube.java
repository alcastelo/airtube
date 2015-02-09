package com.thinktube.airtube;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import com.thinktube.airtube.routing.AirTubeRouterI;
import com.thinktube.airtube.routing.RoutingModuleCommon;
import com.thinktube.net.NetworkInterfaces;
import com.thinktube.net.nio.NIOSelectorThread;

public class AirTube implements AirTubeInterfaceI, MonitorInterfaceI {
	private final static Logger LOG = Logger.getLogger(AirTube.class.getSimpleName());
	private static final boolean LICENCE_CHECK = false;
	private static final int LICENSE_MONTH = Calendar.OCTOBER;
	private static final int LICENSE_YEAR = 2014;
	private NIOSelectorThread selector;
	private AirTubeRegistry registry;
	private AirTubeRouterI router;
	private SubscriptionManager sm;
	private DataTransmit dt;
	private static MonitorCallbackI mon;
	private Set<AirTubeConnectionCallbackI> components = new HashSet<AirTubeConnectionCallbackI>();
	private NetworkInterfaces nifs;
	private boolean started = false;

	public AirTube() {
		/* start with random generated deviceId */
		this(0);
	}

	public AirTube(long deviceId) {
		LOG.setLevel(java.util.logging.Level.FINE);

		if (deviceId == 0) {
			deviceId = new Random().nextLong();
		}

		selector = new NIOSelectorThread();
		registry = new AirTubeRegistry(deviceId);
		router = new RoutingModuleCommon(registry);
		sm = new SubscriptionManager(registry);
		dt = new DataTransmit(selector, registry, sm, router);
		mon = new DummyMonitor();

		LOG.info("AirTube created with deviceID " + Long.toHexString(deviceId));
	}

	public void start() {
		start(null); /* all network interfaces */
	}

	public void start(NetworkInterfaces ifs) {

		/* simple license expiry */
		if (LICENCE_CHECK) {
			Calendar c = Calendar.getInstance();
			if (c.get(Calendar.MONTH) >= LICENSE_MONTH || c.get(Calendar.YEAR) > LICENSE_YEAR) {
				LOG.severe("*** Your licence has expired, please contact Thinktube! ***");
				throw new RuntimeException("Your licence has expired, please contact Thinktube!");
			}
		}

		if (ifs == null || ifs.getInterfaces().size() == 0) {
			this.nifs = new NetworkInterfaces();
			this.nifs.addAllInterfaces();
		} else {
			this.nifs = ifs;
		}
		LOG.info("AirTube using interfaces:" + nifs.getNames());
		mon.setInterfaces(nifs.getNetworkInterfaces());

		selector.start();
		try {
			dt.start(nifs);
			router.start(nifs);
			started = true;
			mon.setState(started, router.getOnDemandDVEnabled());

			LOG.info("AirTube started");
		} catch (IOException e) {
			LOG.info("AirTube NOT started!!!");
			e.printStackTrace();
		}
	}

	public void stop() {
		for (AirTubeConnectionCallbackI cb : components) {
			cb.onDisconnect();
		}
		router.stop();
		dt.stop();
		selector.stop();
		started = false;
		mon.setState(started, router.getOnDemandDVEnabled());
	}

	public static MonitorCallbackI getMonitor() {
		return mon;
	}

	/* service interface */

	public AirTubeID registerService(ServiceDescription desc, AirTubeCallbackI cb) {
		if (desc == null || cb == null) {
			LOG.warning("can't register service without description and callback!");
			return null;
		}

		/* default to normal Traffic Class */
		if (desc.tos == null)
			desc.tos = TrafficClass.NORMAL;

		LocalService ls = registry.addLocalService(desc, cb);
		LOG.fine("registered service " + ls);
		router.triggerUpdate();
		return ls.id;
	}

	public boolean unregisterService(AirTubeID sid) {
		if (sid == null) {
			LOG.warning("unregisterServive: service ID can not be null!");
			return false;
		}
		LOG.fine("unregister service " + sid);
		LocalService ls = registry.getLocalService(sid);
		if (ls == null) {
			LOG.warning("unregisterService: service " + sid + " not found!");
			return false;
		}
		ls.unsubscribeAllClients();
		registry.removeLocalService(sid);
		router.triggerUpdate();
		return true;
	}

	@Override
	public void sendServiceData(AirTubeID sid, ServiceData data) {
		if (sid == null || data == null) {
			LOG.warning("sendData: service ID and data can not be null!");
			return;
		}
		LocalService ls = registry.getLocalService(sid);
		if (ls == null) {
			LOG.warning("sendData: service " + sid + " not found!");
			return;
		}
		ls.sendData(data);
	}

	@Override
	public int getNumberOfClients(AirTubeID sid) {
		if (sid == null) {
			LOG.warning("getNumberOfClients: service ID can not be null!");
			return -1;
		}
		LocalService ls = registry.getLocalService(sid);
		if (ls == null) {
			LOG.warning("getNumberOfClients: service " + sid + " not found!");
			return -1;
		}
		return ls.clients.size();
	}

	/* client interface */

	@Override
	public AirTubeID registerClient(AirTubeCallbackI cb) {
		if (cb == null) {
			LOG.warning("can't register client without callback!");
			return null;
		}
		LocalClient lc = registry.addLocalClient(cb);
		LOG.fine("registered client " + lc);
		return lc.id;
	}

	@Override
	public boolean unregisterClient(AirTubeID cid) {
		if (cid == null) {
			LOG.warning("unregisterClient: client ID can not be null!");
			return false;
		}
		LOG.fine("unregister client " + cid);
		LocalClient lc = registry.getLocalClient(cid);
		if (lc == null) {
			LOG.warning("unregisterClient: client " + cid + " not found!");
			return false;
		}
		sm.removePendingSubscriptionsForClient(cid);
		lc.unsubscribeFromAllServices();
		registry.unregisterServiceInterest(lc);
		registry.removeLocalClient(cid);
		return true;
	}

	@Override
	public void subscribeService(AirTubeID sid, AirTubeID cid, ConfigParameters conf) {
		if (cid == null || sid == null) {
			LOG.warning("subscribeService: client and service ID can not be null!");
			return;
		}
		LOG.fine("subscribe client " + cid + " to " + sid);
		LocalClient lc = registry.getLocalClient(cid);
		if (lc == null) {
			LOG.warning("client " + cid + " not found");
			return;
		}
		if (conf != null) {
				lc.config = conf;
		}
		AService as = registry.getService(sid);
		if (as == null) {
			LOG.warning("service " + sid + " not found");
			return;
		}
		as.subscribe(lc);
	}

	@Override
	public void unsubscribeService(AirTubeID sid, AirTubeID cid) {
		if (cid == null || sid == null) {
			LOG.warning("unsubscribeService: client and service ID can not be null!");
			return;
		}
		LOG.fine("unsubscribe client " + cid + " from " + sid);
		LocalClient lc = registry.getLocalClient(cid);
		if (lc == null) {
			LOG.warning("client " + cid + " not found");
			return;
		}
		AService as = registry.getService(sid);
		if (as == null) {
			LOG.warning("service " + sid + " not found");
			return;
		}
		as.unsubscribe(lc);
	}

	@Override
	public void findServices(ServiceDescription desc, AirTubeID cid) {
		if (cid == null || desc == null) {
			LOG.warning("findServices: client ID and ServiceDescription can not be null!");
			return;
		}
		LocalClient lc = registry.getLocalClient(cid);
		if (lc == null) {
			LOG.warning("findServices: client " + cid + " not found!");
			return;
		}
		registry.registerServiceInterest(desc, lc);
	}

	@Override
	public void unregisterServiceInterest(ServiceDescription desc, AirTubeID cid) {
		if (cid == null || desc == null) {
			LOG.warning("unregisterServiceInterest: client ID and ServiceDescription can not be null!");
			return;
		}
		LocalClient lc = registry.getLocalClient(cid);
		if (lc == null) {
			LOG.warning("registerServiceInterest: client " + cid + " not found!");
			return;
		}
		registry.unregisterServiceInterest(desc, lc);
	}

	@Override
	public void sendData(AirTubeID toId, AirTubeID fromId, ServiceData data) {
		if (toId == null || fromId == null || data == null) {
			LOG.warning("to and from ID and data can not be null!");
			return;
		}
		// from client -> service
		AService as = registry.getService(toId);
		if (as != null) {
			as.receiveData(fromId, data);
			return;
		}
		// from service -> client
		as = registry.getService(fromId);
		if (as != null) {
			AClient ac = as.getClient(toId);
			if (ac != null) {
				ac.receiveData(fromId, data, as.desc.type, as.desc.tos);
			} else {
				LOG.warning("client " + toId + " not found");
			}
		} else {
			LOG.warning("service " + fromId + " and " + toId + " not found");
		}
	}

	@Override
	public ServiceDescription getDescription(AirTubeID sid) {
		if (sid == null) {
			LOG.warning("service ID can not be null!");
			return null;
		}
		AService as = registry.getService(sid);
		if (as == null) {
			LOG.warning("service " + sid + " not found!");
			return null;
		}
		return as.desc;
	}

	/* monitor interface */

	@Override
	public void registerMonitor(MonitorCallbackI moni) {
		if (moni == null) {
			LOG.warning("can't register null monitor!");
			return;
		}
		LOG.warning("registering monitor");
		mon = moni;

		/* notify monitor of all existing stuff */

		mon.setDeviceID(registry.deviceId);
		mon.setState(started, router.getOnDemandDVEnabled());

		if (!started) {
			return;
		}

		mon.setInterfaces(nifs.getNetworkInterfaces());

		for (AService as: registry.getLocalServices()) {
			mon.addService(as.id, as.desc, MonitorCallbackI.Location.LOCAL);
			as.notifyAllSubscriptions(mon);
		}
		for (LocalClient lc : registry.getLocalClients()) {
			mon.addClient(lc.id, MonitorCallbackI.Location.LOCAL);
		}
		for (Peer p : registry.getAllPeers()) {
			mon.addPeer(p.deviceId);
			for (AService as: p.getServices()) {
				mon.addService(as.id, as.desc, MonitorCallbackI.Location.REMOTE);
				as.notifyAllSubscriptions(mon);
			}
			for (RemoteClient rc : p.getClients()) {
				mon.addClient(rc.id, MonitorCallbackI.Location.REMOTE);
			}
		}
	}

	@Override
	public void unregisterMonitor() {
		LOG.warning("unregistering monitor");
		mon = new DummyMonitor(); // we always want to be able to call its methods...
	}

	@Override
	public void clearAll() {
		registry.clearAll();
		router.clear();
		sm.clear();
	}

	@Override
	public void addComponent(AirTubeConnectionCallbackI comp) {
		components.add(comp);
		comp.onConnect(this);
	}

	@Override
	public void removeComponent(AirTubeConnectionCallbackI comp) {
		components.remove(comp);
	}

	@Override
	public void setProxy(InetAddress proxy) {
		router.setProxy(proxy);
	}

	@Override
	public void testFunction() {
		// Temporarily set to what ever test function desired
		router.testFunction();
	}

	@Override
	public void tracerouteStart(long dst, int type, int interval) {
		router.tracerouteStart(dst, type, interval);
	}

	@Override
	public void tracerouteStop() {
		router.tracerouteStop();
	}

	@Override
	public void setOnDemandDV(boolean b) {
		router.setOnDemandDV(b);
	}
}
