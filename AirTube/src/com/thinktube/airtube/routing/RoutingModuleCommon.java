package com.thinktube.airtube.routing;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import com.thinktube.airtube.AirTubeRegistry;
import com.thinktube.airtube.DataTransmit;
import com.thinktube.airtube.IpConnectionCache;
import com.thinktube.airtube.TrafficClass;
import com.thinktube.airtube.TransmissionType;
import com.thinktube.airtube.routing.dv.DistanceVectorRouting;
import com.thinktube.airtube.routing.nbr.NbrLink;
import com.thinktube.airtube.routing.nbr.NeighbourManager;
import com.thinktube.airtube.routing.nbr.NbrPeer;
import com.thinktube.net.NetworkInterfaces;
import com.thinktube.net.nio.PktConnection;
import com.thinktube.airtube.routing.dv.OnDemandDV;

public class RoutingModuleCommon implements AirTubeRouterI {
	private final static Logger LOG = Logger.getLogger(RoutingModuleCommon.class.getSimpleName());
	protected static final long TIMEOUT_INTERVAL = 1000;  /* better to be < 500ms, but now 1000ms for debugging */
	private final DistanceVectorRouting dv;
	private final OnDemandDV od;
	private final Traceroute trace;
	private final NeighbourManager nbrMgr;
	private final ProxyManager proxy;

	/** The switch to enable OD protocol */
	private boolean onDemandDVEnabled = true;

	private short mySeqNo = (short) ((System.currentTimeMillis()/DistanceVectorRouting.DV_UPDATE_INTERVAL_MS) & 0x7fff);

	long lastTimeoutTime = 0;

	private NetworkInterfaces nifs;

	public RoutingModuleCommon(AirTubeRegistry registry) {
		nbrMgr = new NeighbourManager(registry.deviceId, this);
		dv = new DistanceVectorRouting(registry, this);
		od = new OnDemandDV(registry.deviceId, this, dv);
		trace = new Traceroute(registry.deviceId);
		proxy = new ProxyManager();
	}

	public void checkTimer() {
		long curTime = System.currentTimeMillis();

		if (curTime > lastTimeoutTime + TIMEOUT_INTERVAL) {
			timeout();
		}

		dv.sendPacket(nifs, false);
		nbrMgr.sendPacket(nifs);
	}

	@Override
	public void start(NetworkInterfaces nifs) {
		this.nifs = nifs;
		proxy.start(nifs);
		nbrMgr.start(nifs);
		dv.start(nifs);

		/* register packet reception handlers */
		DataTransmit.getInstance().setHandler(NeighbourManager.NBR_PKT_TYPE, nbrMgr);
		DataTransmit.getInstance().setHandler(DistanceVectorRouting.DV_PKT_TYPE, dv);
		DataTransmit.getInstance().setHandler(OnDemandDV.OD_PKT_TYPE, od);
		DataTransmit.getInstance().setHandler(Traceroute.TRACE_PKT_TYPE, trace);
	}

	@Override
	public void stop() {
	}

	@Override
	public synchronized void triggerUpdate() {
		LOG.info("TRIGGER update!");
		dv.sendPacket(nifs, true);
	}

	@Override
	/* this is the default, used by data traffic */
	public synchronized IpConnectionCache getNextHopCC(long deviceId) {
		return getNextHopCC(deviceId, false);
	}

	/** this is the internal implementation which can be used to flag control packets */
	private synchronized IpConnectionCache getNextHopCC(long deviceId, boolean controlPacket) {
		Route r = dv.getRoute(deviceId);

		if (onDemandDVEnabled) {
			/*
			 * notify the existence of the active flow to on-demand protocol.
			 * also request to explore the route to the dst if necessary
			 */
			if (controlPacket == false) {
				od.updateActiveFlow(deviceId);
				if (r == null || !r.isUP()) {
					/* route not found or not UP */
					od.routeExplore(deviceId);
					return null;
				}
			}
		}

		if (r != null && r.isUP()) {
			NbrPeer nbr = nbrMgr.getNeighbour(r.nextHop);
			if (nbr == null)
				return null;
			NbrLink nl = nbr.getBestLink();
			if (nl != null) {
				return nl.getConnectionCache();
			}
		}
		return null;
	}

	@Override
	public void setProxy(InetAddress ip) {
		proxy.setProxy(ip);
	}

	@Override
	public synchronized void clear() {
		dv.clear();
		nbrMgr.clear();
		proxy.clear();
	}

	private synchronized void timeout() {
		nbrMgr.timeout();
		od.timeout();
		dv.timeout();
	}

	public synchronized void addNbr(NbrPeer nbr) {
		dv.addNbr(nbr);
	}

	public synchronized void updateNbr(NbrPeer nbr) {
		dv.updateNbr(nbr);
	}

	public synchronized void removeNbr(NbrPeer nbr) {
		dv.removeNbr(nbr);
	}

	@Override
	public boolean notifyDataReceptionFrom(long deviceId, InetAddress inetAddress, PktConnection conn) {
		return nbrMgr.notifyDataReceptionFrom(deviceId, inetAddress, conn);
	}

	public short getSeqNo() {
		return mySeqNo;
	}

	public short incrementSeqNo() {
		mySeqNo += 1;
		mySeqNo &= 0x7fff;
		return mySeqNo;
	}

	/**
	 * Test if route update is valid
	 *
	 * This test was only based on seqNo but is enhanced by age, so maybe it
	 * should be renamed?
	 *
	 * @param S1
	 *            NEW seqNum of incoming route update
	 * @param S2
	 *            OLD seqNum we have recorded for this route
	 * @param age1
	 *            NEW age (ms) of incoming route update (corresponding to S1)
	 * @param age2
	 *            OLD age (ms) we have recorded for this route (corresponding to
	 *            S2)
	 * @return true to accept route update, false to ignore it
	 */
	public boolean seqNoGreater(long S1, long S2, int age1, int age2) {
		/*
		 * Taken from OLSR: http://www.olsr.org/docs/report_html/node104.html
		 * 
		 * Note: upper bound of age field is now 2^31, which is 546 hour, so we
		 * do not have to worry abut wrap up to 0 (or negative number) because
		 * routes will time out much earlier
		 *
		 * AGE_DELAY_TOLERANCE_MS is used to take account of the considerable
		 * accumulated delay (processing delay and propagation delay) at each
		 * relay node. It assumes DV/OD control packets are never queued for
		 * a long time in lower layers.
		 */
		if ((age2 - age1) > DistanceVectorRouting.DV_AGE_DELAY_TOLERANCE_MS) {
			LOG.finer("seqNoGreater " + S1 + "/" + age1 + "ms vs " + S2 + "/" + age2 + "ms is: true by age");
			return true;
		} else if ((age1 - age2) > DistanceVectorRouting.DV_AGE_DELAY_TOLERANCE_MS) {
			LOG.finer("seqNoGreater " + S1 + "/" + age1 + "ms vs " + S2 + "/" + age2 + "ms is: false by age");
			return false;
		} else {
			if (((S1 > S2) && (S1 - S2 <= DistanceVectorRouting.DV_SEQNO_EXP_WINDOW)) ||
				((S2 > S1) && (S2 - S1 > DistanceVectorRouting.DV_SEQNO_EXP_WINDOW))) {
				LOG.finer("seqNoGreater " + S1 + "/" + age1 + "ms vs " + S2 + "/" + age2 + "ms is: true by seqNo");
				return true;
			} else {
				LOG.finer("seqNoGreater " + S1 + "/" + age1 + "ms vs " + S2 + "/" + age2 + "ms is: false by seqNo");
				return false;
			}
		}
	}

	public boolean isActiveFlow(long deviceId) {
		return od.isActiveFlow(deviceId);
	}

	public NbrPeer getNeighbour(long deviceId) {
		return nbrMgr.getNeighbour(deviceId);
	}

	@Override
	public void testFunction() {
		/*
		 * this is connected with the GUI "Test" button,
		 * you can change this to whatever you like!!!
		 */
		LOG.info("TEST Button");
		od.routeExplore(0xaaL);
	}

	@Override
	public void tracerouteStart(long dst, int type, int interval) {
		LOG.info("START " + (type == 0 ? "Ping" : "Traceroute") + " to " + Long.toHexString(dst) + " int " + interval);
		trace.startTrace(dst, type, interval);
	}

	@Override
	public void tracerouteStop() {
		LOG.info("STOP Tracert");
		trace.stopTrace();
	}

	@Override
	public void setOnDemandDV(boolean b) {
		LOG.info("*** ENABLE OD " + b);
		onDemandDVEnabled = b;
	}

	@Override
	public boolean getOnDemandDVEnabled() {
		return onDemandDVEnabled;
	}

	@Override
	public void queueToSendLater(ByteBuffer[] bufs, long dstDID, TransmissionType type, TrafficClass tos) {
		dv.queueToSendLater(bufs, dstDID, type, tos);
	}
}
