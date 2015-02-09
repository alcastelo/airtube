package com.thinktube.airtube;

import java.net.NetworkInterface;
import java.util.List;

/* Due to the AIDL limitation that it can not subclass or implement interfaces,
 * this needs to be manually kept in sync with android/MonitorCallback.aidl */

/**
 * Callback for Monitor components.
 * <p>
 * Monitor components can monitor the internal state of AirTube and once
 * registered with AirTube will receive the callbacks listed below.
 * <p>
 * Note: most API users don't need this!
 *
 * @see MonitorInterfaceI
 */
public interface MonitorCallbackI {
	enum Location { LOCAL, REMOTE };
	/**
	 * Service has been added
	 * 
	 * @param id
	 *            The ID of the service
	 * @param desc
	 *            description of the service
	 * @param type
	 *            local or remote
	 */
	void addService(AirTubeID id, ServiceDescription desc, Location type);

	/**
	 * Service has been removed
	 * 
	 * @param id
	 *            ID of the service
	 */
	void removeService(AirTubeID id);

	/**
	 * Client has been added
	 * 
	 * @param id
	 *            ID of the client
	 * @param type
	 *            local, remote
	 */
	void addClient(AirTubeID id, Location type);

	/**
	 * Client has been removed
	 * 
	 * @param id
	 *            ID of the client
	 */
	void removeClient(AirTubeID id);

	/**
	 * Client subscribed to service
	 * 
	 * @param serviceId
	 *            ID of service
	 * @param clientId
	 *            ID of client
	 */
	void addSubscription(AirTubeID serviceId, AirTubeID clientId);

	/**
	 * Client has unsubscribed from service
	 * 
	 * @param serviceId
	 *            ID of service
	 * 
	 * @param clientId
	 *            ID of client
	 */
	void removeSubscription(AirTubeID serviceId, AirTubeID clientId);

	/**
	 * A peer has been found. A peer is any node also running AirTube in the
	 * same network.
	 * 
	 * @param deviceId
	 *            ID of Peer
	 */
	void addPeer(long deviceId);

	/**
	 * Peer has been lost. Most likely disconnected.
	 * 
	 * @param deviceId
	 *            ID of the peer
	 */
	void removePeer(long deviceId);

	/**
	 * Update Peer info, mostly used for debugging.
	 * 
	 * @param did
	 *            ID of the peer
	 *
	 * @param info
	 *            An arbitrary info string
	 */
	void updatePeer(long did, long lastTime, String info);

	/**
	 * Update list of interfaces in use
	 * 
	 * @param intf
	 *            The new list
	 */
	void setInterfaces(List<NetworkInterface> intf);

	/**
	 * Device ID set
	 * 
	 * @param deviceId
	 *            device ID
	 */
	void setDeviceID(long deviceId);

	/**
	 * Set state of Airtube
	 *
	 * @param started or not
	 * @param onDemandEnabled On-Demand DV part enabled
	 */
	void setState(boolean started, boolean onDemandEnabled);

	/**
	 * Result of a Traceroute or Ping
	 *
	 * @param trace
	 */
	void traceRouteResult(String[] trace);
}
