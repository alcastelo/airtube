package com.thinktube.airtube.routing.dv;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.thinktube.airtube.AirTubeRegistry;
import com.thinktube.airtube.DataTransmit;
import com.thinktube.airtube.TrafficClass;
import com.thinktube.airtube.TransmissionType;
import com.thinktube.airtube.routing.Metric;
import com.thinktube.airtube.routing.MetricETT;
import com.thinktube.airtube.routing.Route;
import com.thinktube.airtube.routing.RoutingModuleCommon;
import com.thinktube.airtube.routing.ServicePacketFmt;
import com.thinktube.airtube.routing.nbr.NbrLink;
import com.thinktube.airtube.routing.nbr.NbrPeer;
import com.thinktube.net.InetAddrUtil;
import com.thinktube.net.NetworkInterfaces;
import com.thinktube.net.NetworkInterfaces.NetIf;
import com.thinktube.net.nio.PktConnection;

public class DistanceVectorRouting implements DataTransmit.PacketHandler {
	private final static Logger LOG = Logger.getLogger(DistanceVectorRouting.class.getSimpleName());

	public static final byte DV_PKT_TYPE = 4;

	/** The interval for the DV update messages (2 sec) */
	public static final long DV_UPDATE_INTERVAL_MS = 2000;

	/** The default time we allow the DOWN_PENDING state (updated by OD) */
	protected static final long DV_DOWN_PENDING_PROBATION_MS = DV_UPDATE_INTERVAL_MS * 2;

	/** After this timeout a route is considered down (30 sec) */
	protected static final long DV_ROUTE_DOWN_TIMEOUT_MS = 30000;

	/** After this timeout we remove a route (2.5 min) */
	protected static final long DV_ROUTE_REMOVAL_MS = DV_ROUTE_DOWN_TIMEOUT_MS * 5;

	/** After this timeout the peer is removed from the registry (1 min) */
	protected static final long PEER_TIMEOUT_MS = 60000;

	/**
	 * The size of the accepted seqNo window (before: "EXP_VALUE")
	 * now as reset event problem is solved by age, we take MAX/2
	 */
	public static final long DV_SEQNO_EXP_WINDOW = 0x7fff / 2;

	/**
	 * upper bound of the possible maximum error with age value (6 sec)
	 *
	 * The age values can not take propagation and processing delay into
	 * account and this tolerance is introduced to alleviate that
	 */
	public static final int DV_AGE_DELAY_TOLERANCE_MS = 6000;

	/** The maximum anticipated hop-count of the network */
	public static final int DV_MAX_HOP_COUNT = 32;

	/** We don't accept route information which is older than that (96 sec = ~1.5 min) */
	public static final long DV_MAX_ACCEPTED_AGE = DV_UPDATE_INTERVAL_MS * DV_MAX_HOP_COUNT;

	private final AirTubeRegistry registry;
	private final RoutingModuleCommon router;
	private boolean sendImmediateFlag;

	private ConcurrentHashMap<Long, DVPeer> peers = new ConcurrentHashMap<Long, DVPeer>();

	private long lastSendTime;

	private NetworkInterfaces nifs;

	public DistanceVectorRouting(AirTubeRegistry registry, RoutingModuleCommon router) {
		this.registry = registry;
		this.router = router;
	}

	public void clear() {
		peers.clear();
	}

	public Route getRoute(long deviceId) {
		DVPeer dp = peers.get(deviceId);
		if (dp == null) {
			LOG.warning("peer [" + Long.toHexString(deviceId) + "] not found");
			return null;
		}
		return dp.getBestRoute();
	}

	public void sendPacket(NetworkInterfaces nifs, boolean force) {
		sendPacket(nifs, force, DVPeer.Flag.NORMAL);
	}

	public void sendPacket(NetworkInterfaces nifs, boolean force, DVPeer.Flag flag) {
		long curTime = System.currentTimeMillis();
		if (!force && curTime < lastSendTime + DV_UPDATE_INTERVAL_MS) {
			return;
		}

		router.incrementSeqNo();
		LOG.fine("sending packet (" + router.getSeqNo() + ") after " + (System.currentTimeMillis() - lastSendTime) / 1000f + " sec");
		lastSendTime = curTime;

		/* build a different packet for each interface, this is necessary for DV split-horizon */
		for (NetIf ni : nifs.getInterfaces()) {
			if (!ni.isMobile()) {
				ByteBuffer pkt = getPacket(ni, flag);
				if (pkt != null) {
					DataTransmit.getInstance().sendBroadcastOnInterface(ni, pkt, TrafficClass.VOICE);
				}
			}
		}

		/* after sending a immediate update we clear the flag */
		if (flag == DVPeer.Flag.SEND_IMMEDIATE) {
			for (DVPeer dp : peers.values()) {
				if (dp.isFlag(DVPeer.Flag.SEND_IMMEDIATE)) {
					dp.setFlag(DVPeer.Flag.NORMAL);
				}
			}
		}
	}

	public ByteBuffer getPacket(NetIf ni, DVPeer.Flag flag) {
		/* don't use split-horizon on wireless (possibly adhoc) interfaces */
		boolean splitHorizon = ni == null ? true : !ni.isWireless();
		return buildPacket(ni.getBroadcast(), ni.getNetworkPrefixLength(), splitHorizon, flag);
	}

	/** prepare and build packet for outgoing IP of interface */
	private ByteBuffer buildPacket(InetAddress bc, int netmask, boolean splitHorizon, DVPeer.Flag flag) {
		LOG.fine("sending DV (" + router.getSeqNo() + ") on " + bc + " split-horizon: " + splitHorizon + " flag: " + flag);

		/* prepare list of peers */
		ArrayList<DVPeer> inPeers = new ArrayList<DVPeer>();
		for (DVPeer dp : peers.values()) {
			Route rt = dp.getBestRoute();
			/*
			 * do not propagate if peer is in DOWN_PENDING or TIMEOUT state, but
			 * we do want to propagate DOWN state (METRIC_INFINTE)
			 * TODO: route may be null for DOWN state peer! here we ignore this case
			 */
			if (rt == null || dp.isState(DVPeer.State.DOWN_PENDING) || dp.isState(DVPeer.State.TIMED_OUT)) {
				continue;
			}

			/* select peers based on flag if given; we allow all if flag is NORMAL */
			if (flag != DVPeer.Flag.NORMAL && !dp.isFlag(flag)) {
				continue; // ignore peer with non matching flag
			}

			if (splitHorizon) {
				/*
				 * "split horizon": don't advertise nodes going out thru the same interface
				 */
				NbrPeer nbr = router.getNeighbour(rt.nextHop);
				if (nbr == null)
					continue;

				NbrLink nl = nbr.getBestLink();
				if (nl == null)
					continue;
				if (!InetAddrUtil.isInRage(nl.getIp(), bc, netmask)) {
					inPeers.add(dp);
				}
			} else {
				/* not split horizon, just add all peers */
				inPeers.add(dp);
			}
		}

		return createPacket(inPeers);
	}

	/** actually write packet data to buffer */
	private ByteBuffer createPacket(ArrayList<DVPeer> inPeers) {
		/*
		 * if there are no peers and no local services we have nothing to send.
		 * Except maybe for a new seqNo, but we don't use this right now...
		 */
		if (inPeers.size() == 0 && registry.getLocalServices().size() == 0) {
			return null;
		}

		ByteBuffer buf = ByteBuffer.allocate(1500);

		buf.put(DV_PKT_TYPE);
		buf.putLong(registry.deviceId);
		buf.putShort(router.getSeqNo());

		/* list of local services */
		ServicePacketFmt.writeServices(buf, registry.getLocalServices());

		buf.putInt(inPeers.size());
		for (DVPeer dp : inPeers) {
			Route rt = dp.getBestRoute();
			buf.putLong(dp.deviceId);
			/* don't use lastSeq of the peer, rather the seqNo for the route {peer, lasthop} */
			buf.putShort(rt.seqNo);
			/* use METRIC_INFINITE for DOWN state route */
			if (rt.isUP()) {
				rt.metric.writeTo(buf);
			} else {
				rt.metric.getInfinite().writeTo(buf);
			}
			buf.putLong(rt.lastHop);
			buf.putInt(rt.getCurrentAge());
			LOG.finer("  put DV " + rt);

			/* list of remote peer services */
			ServicePacketFmt.writeServices(buf, dp.peer.getServices());
		}

		buf.flip();
		return buf;
	}

	protected void scheduleSendImmediate() {
		sendImmediateFlag = true;
	}

	private void sendImmediateUpdate() {
		if (!sendImmediateFlag) {
			return;
		}

		// TODO: Add some rate-limiting here. We should not send too many
		// immediate updates in a row, and could rather wait and combine
		// updates.

		LOG.info("Send IMMEDIATE DV update");
		sendPacket(nifs, true, DVPeer.Flag.SEND_IMMEDIATE);
		sendImmediateFlag = false;
	}

	public DVPeer updatePeer(long did, short seqNo, Metric metric, NbrPeer nextHop, long lastHop, int age) {
		/*
		 * Basic validity checks before we even bother to create a peer
		 */
		if (age > DV_MAX_ACCEPTED_AGE) {
			LOG.warning("Ignoring old age (" + age + "ms) route to " + Long.toHexString(did) + " from " + nextHop);
			return null;
		}

		/*
		 * "Whatever you tell me I'm not going to participate in a routing loop!"
		 * 
		 * I can only be the lastHop for a direct neighbour. If someone asks me
		 * to route to a dst over him, but I myself am recorded as last hop this
		 * can only lead to a routing loop.
		 */
		if (lastHop == registry.deviceId && did != nextHop.deviceId) {
			LOG.fine("Ignoring route to " + Long.toHexString(did) + " with myself as lastHop, from " + nextHop);
			return null;
		}

		DVPeer dp = peers.get(did);
		if (dp == null) {
			dp = new DVPeer(did, registry, this, router);
			/* registry.addPeer is executed in DVPeer class constructor */
			peers.put(did, dp);
		}

		/*
		 * validity check with seqNo, metric is done inside the called function
		 *
		 * the route is updated only if this is verified as "loop-free" and
		 * "latest" information for the {dest, lastHop}
		 */
		boolean updateAccepted = dp.updateRoute(nextHop, metric, seqNo, lastHop, age);
		if (updateAccepted) {
			return dp;
		}
		return null;
	}

	@Override
	public void receive(ByteBuffer in, InetAddress from, PktConnection conn) {
		long fromDID = in.getLong();

		NbrPeer fromNbr = router.getNeighbour(fromDID);
		if (fromNbr == null || fromNbr.deviceId == registry.deviceId) {
			return;
		}

		short pktSeqNo = in.getShort();
		LOG.fine("received DV from " + fromNbr + " (" + pktSeqNo + ")");

		/*
		 * at first, update the Peer entry of the NbrPeer which sends this message to me
		 * age is 0 because this comes from my direct nbr
		 */
		DVPeer dp = updatePeer(fromNbr.deviceId, pktSeqNo, null, fromNbr, registry.deviceId, 0);

		/* services of sender */
		if (ServicePacketFmt.readServices(in, dp == null ? null : dp.peer, pktSeqNo)) {
			LOG.info("services changed! " + dp);
			dp.markForImmediateUdate();
		}

		/* list of peers */
		int size = in.getInt();
		for (int i = 0; i < size; i++) {
			long peerDID = in.getLong();
			short peerSeq = in.getShort();
			Metric metric = MetricETT.readFrom(in);
			long lastHopDID = in.getLong();
			int age = in.getInt();

			LOG.finer(" read DV " + Long.toHexString(peerDID) + " seqno=" + peerSeq + " age=" + age +
					" metric=" + metric + " lasthop=" + Long.toHexString(lastHopDID));

			if (peerDID != registry.deviceId) {
				dp = updatePeer(peerDID, peerSeq, metric, fromNbr, lastHopDID, age);
			} else {
				/* Just ignore the rumors they tell about myself */
				dp = null;
			}

			/* This function will always read the packet, but it ignores updates if peer is null */
			if (ServicePacketFmt.readServices(in, dp == null ? null : dp.peer, peerSeq)) {
				LOG.info("services changed! " + dp);
				dp.markForImmediateUdate();
			}
		}

		/*
		 * The processing above may result in the need to send an immediate
		 * update, especially when the list contained METRIC_INFINITE routes
		 * or the list of services changed
		 */
		sendImmediateUpdate();
	}

	public void timeout() {
		for (DVPeer dp : peers.values()) {
			/*
			 * first call the timeout function of the peer, which may timeout
			 * routes and may change the peer's state to DOWN, DOWN_PENDING or
			 * TIMED_OUT
			 */
			dp.timeout();

			if (dp.isState(DVPeer.State.TIMED_OUT) && !dp.hasRoutes()) {
				/*
				 * We kept the peer around for a while after timeout, to keep
				 * old route information to avoid count-to-infinity, but there
				 * is no point in doing so when there are no routes left and
				 * DVPeer is just "empty".
				 */
				LOG.info("### remove " + dp);
				peers.remove(dp.deviceId);
			}
		}

		/*
		 * The above may have resulted in the need to send an immediate update
		 * (the decision of this is in dp.selectBestRoute() which may be called
		 * thru dp.timout())
		 */
		sendImmediateUpdate();
	}

	public void addNbr(NbrPeer nbr) {
		LOG.info("add Nbr " + nbr);
		/*
		 * add DV representation of the NBR, or a direct route to it if we
		 * already know this peer
		 * 
		 * lastHop to a Nbr node is myself, metric 0, age 0
		 */
		DVPeer dp = peers.get(nbr.deviceId);
		if (dp == null) {
			dp = new DVPeer(nbr.deviceId, registry, this, router);
			dp.updateRoute(nbr, null, nbr.getLastSeq(), registry.deviceId, 0);
			LOG.fine("created new " + dp);
			peers.put(dp.peer.deviceId, dp);
		} else {
			/*
			 * Peer may have been known before via longer routes, now we add a
			 * direct link
			 */
			dp.updateRoute(nbr, null, nbr.getLastSeq(), registry.deviceId, 0);
			LOG.fine("updated " + dp);
		}
	}

	public void updateNbr(NbrPeer nbr) {
		LOG.fine("update Nbr " + nbr);
		DVPeer dp = peers.get(nbr.deviceId);
		if (dp == null) {
			LOG.warning("attempt to update unknown NBR");
			throw new RuntimeException("Unknown NBR");
		} else {
			/* lastHop to a Nbr node is myself, metric 0, age 0*/
			dp.updateRoute(nbr, null, nbr.getLastSeq(), registry.deviceId, 0);
		}
	}

	public void removeNbr(NbrPeer nbr) {
		LOG.info("remove Nbr " + nbr);
		/*
		 * now, we have lost direct link(s) to this Nbr
		 *
		 * we inform all peers of this fact, which makes all routes going thru
		 * this Nbr down
		 */
		for (DVPeer dp : peers.values()) {
			dp.nbrRoutesDown(nbr.deviceId);
		}
		/*
		 * If this resulted in a change of best routes, it may be necessary to
		 * send an immediate update
		 * (the decision of this is in dp.selectBestRoute())
		 */
		sendImmediateUpdate();
	}

	protected DVPeer getDVPeer(long deviceId) {
		return peers.get(deviceId);
	}

	/**
	 * checks if the peer is in the route towards the destination
	 * 
	 * @param check
	 *            deviceId of the supposed "upstream peer"
	 * @param dst
	 *            deviceId of the destination
	 * @return true or false
	 * 
	 *         Note that if "dst" or "check" is "myself" we return false - we
	 *         don't consider ourself to be "upstream" as we are "here".
	 * 
	 *         If "dst" equals "check" we return true - the dst itself is
	 *         considered to be its own "upstream peer" (even though it sounds
	 *         weird it's not wrong and it makes this function more versatile).
	 */
	public boolean isUpstreamPeer(long check, long dst) {
		/* neither can me myself */
		if (dst == registry.deviceId || check == registry.deviceId)
			return false;

		/*
		 * This is here so we are allowed to pass dst.lastHop instead of dst as
		 * well. Also see Javadoc comments above.
		 */
		if (dst == check)
			return true;

		Route rt = getRoute(dst);
		while (rt != null) {
			if (rt.deviceId == check) {
				return true;
			}
			else if (rt.lastHop == registry.deviceId) {
				return false; /* don't look up route to myself */
			}
			rt = getRoute(rt.lastHop);
		}
		return false;
	}

	public String localRouteTrace(long dst) {
		String s = new String();
		Route rt = getRoute(dst);
		if (rt == null) {
			return s;
		}

		int i=0;
		long firstHop = rt.nextHop;
		while (rt != null && rt.lastHop != registry.deviceId) {
			s = Long.toHexString(rt.lastHop) + "->" + s;
			rt = getRoute(rt.lastHop);
			if (i++ > 20)
				break;
		}
		if (rt != null && rt.nextHop != firstHop) {
			s = Long.toHexString(firstHop) + "!" + s;
		}
		if (s.length() > 2) {
			s = s.substring(0, s.length()-2);  // remove last "->";
		}
		return s;
	}

	public void queueToSendLater(ByteBuffer[] bufs, long dst, TransmissionType type, TrafficClass tos) {
		DVPeer dp = peers.get(dst);
		if (dp == null) {
			LOG.warning("can't queue packet for unknown Peer " + Long.toHexString(dst));
			return;
		}
		dp.addToSendQueue(bufs, type, tos);
	}

	public void start(NetworkInterfaces nifs) {
		this.nifs = nifs;
	}
}
