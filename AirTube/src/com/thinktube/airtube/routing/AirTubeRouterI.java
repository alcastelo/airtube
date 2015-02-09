package com.thinktube.airtube.routing;

import java.net.InetAddress;
import java.nio.ByteBuffer;

import com.thinktube.airtube.IpConnectionCache;
import com.thinktube.airtube.TrafficClass;
import com.thinktube.airtube.TransmissionType;
import com.thinktube.net.NetworkInterfaces;
import com.thinktube.net.nio.PktConnection;

/**
 * this is the interface AirTube (esp. DataTransmit) uses to interface with the
 * routing implementation, not to be confused with specialist routing modules,
 * for which we may define an interface later
 */

public interface AirTubeRouterI {

	void start(NetworkInterfaces nifs);

	void stop();

	void triggerUpdate();

	IpConnectionCache getNextHopCC(long deviceId);

	void setProxy(InetAddress proxy);

	void clear();

	boolean notifyDataReceptionFrom(long deviceId, InetAddress inetAddress, PktConnection conn);

	void testFunction();

	void tracerouteStart(long dst, int type, int interval);

	void tracerouteStop();

	void checkTimer();

	void setOnDemandDV(boolean b);

	boolean getOnDemandDVEnabled();

	void queueToSendLater(ByteBuffer[] bufs, long dstDID, TransmissionType type, TrafficClass tos);
}
