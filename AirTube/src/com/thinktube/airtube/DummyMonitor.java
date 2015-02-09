package com.thinktube.airtube;

import java.net.NetworkInterface;
import java.util.List;

public class DummyMonitor implements MonitorCallbackI {
	public void addService(AirTubeID id, ServiceDescription desc, Location type) {};
	public void removeService(AirTubeID id) {};

	public void addClient(AirTubeID id, Location type) {};
	public void removeClient(AirTubeID id) {};

	public void addSubscription(AirTubeID serviceId, AirTubeID clientId) {};
	public void removeSubscription(AirTubeID serviceId, AirTubeID clientId) {};

	public void addPeer(long deviceId) {};
	public void removePeer(long deviceId) {};
	public void updatePeer(long deviceId, long lastTime, String info) {};

	public void setInterfaces(List<NetworkInterface> intf) {};
	public void setDeviceID(long deviceId) {};
	public void setState(boolean started, boolean onDemandEnabled) {};

	public void traceRouteResult(String[] trace) {};
}
