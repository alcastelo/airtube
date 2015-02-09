package com.thinktube.airtube;

import java.net.InetAddress;

/**
 * This is the interface used to register a {@link MonitorCallbackI} and to control
 * AirTube from the "outside".
 * <p>
 * A "Monitor" can be used to access the internal state of AirTube. It will get
 * notifications of most things which happen in AirTube, for example a new
 * service was found, a peer timed out, etc... This can be useful for debugging
 * or logging purposes or to represent AirTube in a GUI.
 * <p>
 * Note: most API users don't need this!
 * 
 * @see MonitorCallbackI
 */
public interface MonitorInterfaceI {

	/**
	 * Register a {@link MonitorCallbackI}. Only one callback can be registered.
	 * 
	 * @param mon
	 *            callback for monitor events
	 */
	void registerMonitor(MonitorCallbackI mon);

	/**
	 * Unregister monitor
	 */
	void unregisterMonitor();

	/**
	 * Used by the monitor to request AirTube to clear all data structures
	 */
	void clearAll();

	/**
	 * Configure a proxy IP address
	 * 
	 * @param ip
	 *            IP address of proxy
	 * */
	void setProxy(InetAddress ip);

	/**
	 * Generic test function to be re-assigned in AT
	 */
	void testFunction();

	/**
	 * Traceroute/ping to be started
	 *
	 * @param dst destination deviceId
	 * @param type ping/traceroute
	 * @param interval interval
	 */
	void tracerouteStart(long dst, int type, int interval);

	/**
	 * Traceroute/ping to be stopped
	 */
	void tracerouteStop();

	/**
	 * Enable or disable On-Demand DV
	 * @param b enabled
	 */
	void setOnDemandDV(boolean b);
}
