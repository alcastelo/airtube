package com.thinktube.airtube.routing.dv;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.thinktube.airtube.AirTube;
import com.thinktube.airtube.AirTubeRegistry;
import com.thinktube.airtube.DataTransmit;
import com.thinktube.airtube.IpConnectionCache;
import com.thinktube.airtube.Peer;
import com.thinktube.airtube.TrafficClass;
import com.thinktube.airtube.TransmissionType;
import com.thinktube.airtube.routing.Metric;
import com.thinktube.airtube.routing.Route;
import com.thinktube.airtube.routing.RoutingModuleCommon;
import com.thinktube.airtube.routing.nbr.NbrPeer;
import com.thinktube.airtube.routing.dv.DistanceVectorRouting;

public class DVPeer {
	private final static Logger LOG = Logger.getLogger(DVPeer.class.getSimpleName());
	private final AirTubeRegistry registry;
	public final Peer peer;
	private long lastTime;
	private final DistanceVectorRouting DVR;
	private final RoutingModuleCommon router;
	public final long deviceId;

	/*
	 * allRoutes contains the candidate routes which are believed to be loop
	 * free. To guarantee the loop free, we use 3-tuple {dst, lastHop, seqNo} to prove
	 * it, therefore, we now use lastHop as the key of the hash table, instead of
	 * nextHop
	 */
	private Map<Long, Route> allRoutes = new HashMap<Long, Route>();
	private Route bestRoute;

	public enum State {
		UP, DOWN_PENDING, DOWN, TIMED_OUT
	};
	private State state = State.UP;

	public enum Flag {
		NORMAL, SEND_IMMEDIATE
	};
	private Flag flag;

	private long downPendingExpireTime = 0;

	private List<SendItem> sendQueue = new LinkedList<SendItem>();

	private static class SendItem {
		ByteBuffer[] bufs;
		TransmissionType type;
		TrafficClass tos;

		SendItem(ByteBuffer[] bufs, TransmissionType type, TrafficClass tos) {
			this.bufs = bufs;
			this.type = type;
			this.tos = tos;
		}
	}

	DVPeer(long deviceId, AirTubeRegistry registry, DistanceVectorRouting dv, RoutingModuleCommon router) {
		LOG.info("create DV peer from did " + Long.toHexString(deviceId));
		this.deviceId = deviceId;
		this.registry = registry;
		this.DVR = dv;
		this.router = router;
		this.flag = Flag.NORMAL;

		this.peer = new Peer(deviceId, registry);
		registry.addPeer(peer);
	}

	@Override
	public String toString() {
		return "DVPeer [" + Long.toHexString(peer.deviceId) + ": " + toGUIString() + "]";
	}

	public String toGUIString() {
		String common = state + " ";
		if (isNeighbour())
			return common + bestRoute.metric + " (" + bestRoute.seqNo + ") direct";
		else if (bestRoute != null)
			return common + bestRoute.metric + " (" + bestRoute.seqNo + ") via " + DVR.localRouteTrace(deviceId);
		else
			return common + "no route";
	}

	public Route getBestRoute() {
		return bestRoute;
	}

	/** select best route
	 *
	 * Depending on the available routes this can change the DVPeer state
	 */
	public void selectBestRoute() {
		Route sel = null;
		State prevState = state;
		boolean upStateRouteExist = false;

		LOG.finer("@@@@@@@@@@ select best route of " + allRoutes.size());

		/*
		 * we have to check seqNo and metric regardless of its state, either UP
		 * or DOWN. It might not make sense for you to select DOWN state route
		 * as the best even though there are UP state route, but this algorithm
		 * is correct to avoid looping.
		 * 
		 * For instance:
		 *   route01 is {seqno=100, metric=3, state=DOWN}
		 *   route02 is {seqno= 99, metric=2, state=UP}
		 * we need to select route01 as the best route here.
		 */
		for (Route rt : allRoutes.values()) {
			if (rt.isUP()) {
				upStateRouteExist = true;
			}

			if (sel == null /* first time */
				|| router.seqNoGreater(rt.seqNo, sel.seqNo, rt.getCurrentAge(), sel.getCurrentAge())
				|| ((rt.seqNo == sel.seqNo) && (rt.metric.isBetterThan(sel.metric)))) {
				sel = rt;
			}
		}

		if (sel != bestRoute) {
			LOG.fine("best route changed from " + bestRoute + " to " + sel);
			bestRoute = sel;
		}

		/*
		 * Note: routes may change state from UP to DOWN, so even if bestRoute
		 * did not change, it's state may have changed, so we always evaluate
		 * below
		 * 
		 * We always have to take care not to override TIMED_OUT state, as this
		 * can only be done by finding an UP route.
		 */

		if (bestRoute == null) {
			/* no route is available now */
			if (state != State.TIMED_OUT && state != State.DOWN) {
				state = State.DOWN;
				LOG.info("no route available -> DOWN " + this);
			}
		} else {
			/*
			 * caution: bestRoute can be either UP or DOWN state
			 * 
			 * check if we have UP state non-best route even while the best
			 * route is DOWN state - we hope that this backup route may soon
			 * become the best route by receiving fresh DV update message.
			 * DOWN_PENDING will change to DOWN after some probation period
			 */
			if (!bestRoute.isUP() && upStateRouteExist &&
					state != State.DOWN_PENDING && state != State.TIMED_OUT) {
				LOG.info("best route is down " + state + " -> DOWN_PENDING " + this);
				state = State.DOWN_PENDING;
				/*
				 * DOWN_PENDING_PROBATION_MS is the default time length taking
				 * account of when next fresh DV update message will arrive.
				 * this value will be overridden with short length if OD
				 * protocol schedules route explore an active flow
				 */
				downPendingExpireTime = System.currentTimeMillis() + DistanceVectorRouting.DV_DOWN_PENDING_PROBATION_MS;
			}
			/*
			 * Otherwise if bestRoute is down -> DOWN. We transition to DOWN
			 * also from DOWN_PENDING if the condition for it (best route down
			 * but UP route exists) is not met any more.
			 */
			else if (bestRoute.isDOWN() && !upStateRouteExist && state != State.TIMED_OUT) {
				state = State.DOWN;
			}
			/*
			 * UP is the only state we allow from all states including TIMED_OUT
			 */
			else if (bestRoute.isUP()) {
				state = State.UP;
				/*
				 * if the peer was TIMED_OUT and now we found a new valid route,
				 * we have to add it to the registry again.
				 */
				if (prevState == State.TIMED_OUT) {
					LOG.info("re-add peer " + peer);
					registry.addPeer(peer);
				}
				/*
				 * send queued packets if any
				 */
				if (prevState != State.UP && sendQueue.size() > 0) {
					sendQueuedPackets();
				}
			}

			/*
			 * if the state changed, we may want to send a immediate update
			 * depending on wether there is an active flow. We should not send
			 * DV update message if current state is DOWN_PENDING
			 */
			if (prevState != state && state != State.DOWN_PENDING) {
				LOG.info("state changed from " + prevState + " -> " + state + " for " + this);
				sendImmediateUpdateIfActiveFlow();
			}
		}

		AirTube.getMonitor().updatePeer(deviceId, lastTime, toGUIString());
	}

	private void sendImmediateUpdateIfActiveFlow() {
		if (router.isActiveFlow(this.deviceId)) {
			LOG.fine("  has active flow -> immdiate update");
			markForImmediateUdate();
		}
	}

	private void sendQueuedPackets() {
		Iterator <SendItem> it = sendQueue.iterator();
		IpConnectionCache cc = router.getNextHopCC(deviceId);
		if (cc == null) {
			LOG.warning("could not send queued packets, no cc");
			return;
		}
		while (it.hasNext()) {
			SendItem ri = it.next();
			try {
				LOG.info("sending queued packet to " + this);
				DataTransmit.getInstance().send(ri.bufs, cc, ri.type, ri.tos);
				it.remove();
			} catch (ClosedChannelException e) {
				LOG.warning("could not send queued packet! channel closed");
				// in this case it was not removed and kept for later
			}
		}
	}

	/** update route to peer, returns true only if the update was accepted */
	public boolean updateRoute(NbrPeer nextHop, Metric metric, short seqNo, long lastHop, int age) {
		if (nextHop.getBestLink() == null) {
			throw new RuntimeException("NBR link should not be null!");
		}

		if (metric == null) { // use NBR metric
			metric = nextHop.getBestLink().getMetric();
		} else if (!metric.isInfinite()) {
			metric.add(nextHop.getBestLink().getMetric());
		}

		/*
		 * we are verifying seqNo per {dst, lastHop} instead of conventional {dst}.
		 * Here in DVPeer we are {dst} so this means we index routes by {lastHop}
		 */
		Route rt = allRoutes.get(lastHop);

		/*
		 * Conditions to add/update a route:
		 *
		 * 1.) Always add unknown/new routes
		 *
		 * 2.) Update existing routes according to DV condition:
		 *     2a.) seqNo greater
		 *     2b.) same seqNo and smaller metric
		 *
		 * Here we compare seqNo also with DOWN state entry, otherwise
		 * count-infinity problem happens.
		 *
		 * 3.) Allow METRIC_INFINITE to override if the nextHop is the same.
		 * This is because we do not increment the seqNo on route breaks, so the
		 * update with METRIC_INFINITE will have the same seqNo as the last
		 * regular update. Note that METRIC_INFINITE is also accepted for any
		 * next hop if it satisfies condition 2a.) seqNo greater or condition
		 * 1.) previously unknown routes.
		 */
		if (rt == null														// 1.)
			|| router.seqNoGreater(seqNo, rt.seqNo, age, rt.getCurrentAge())// 2a.)
			|| (seqNo == rt.seqNo && metric.isBetterThan(rt.metric))		// 2b.)
			|| (metric.isInfinite()											// 3.)
				&& rt.nextHop == nextHop.deviceId))							// 3.)
		{
			if (rt == null || rt.nextHop != nextHop.deviceId) {
				/*
				 * New route or
				 *
				 * Different route: last hop is the same, but next hop is
				 * different. We only keep one route per last hop, but we don't
				 * allow to change nextHop, so we create a new Route and replace
				 * the old one.
				 */
				rt = new Route(this.deviceId, lastHop, nextHop.deviceId, metric, seqNo, age);
				Route old = allRoutes.put(lastHop, rt);
				if (old == null) {
					LOG.fine("add " + rt);
				} else {
					LOG.fine("replaced " + old + " with " + rt);
				}
			} else {
				/*
				 * normal update: Route state (UP/DOWN) is taken care in update
				 */
				rt.update(metric, seqNo, age);
				LOG.fine("updated " + rt);
			}

			/*
			 * Update last time we heard anything about this node only when we
			 * did not receive a route retraction - otherwise it would not
			 * expire
			 */
			if (!metric.isInfinite()) {
				lastTime = System.currentTimeMillis();
			}

			/*
			 * Some Route changed, so let's select the best again
			 * TODO: maybe possible to optimize (run conditionally)?
			 */
			selectBestRoute();
			return true;
		} else {
			/*
			 * The update is ignored
			 */
			LOG.fine("IGNORE dst=" + Long.toHexString(deviceId)
					+ " seqNo=new(" + seqNo + ") cur(" + rt.seqNo + ")"
					+ " metric new(" + metric + ") cur(" + rt.metric + ")"
					+ " lastHop=" + Long.toHexString(lastHop));
			return false;
		}
	}

	public void timeout() {
		long curTime = System.currentTimeMillis();
		boolean bestRouteDown = false;

		/*
		 * time out routes
		 */
		Iterator<Entry<Long, Route>> it = allRoutes.entrySet().iterator();
		while (it.hasNext()) {
			Route rt = it.next().getValue();
			if (rt.getLastTime() + DistanceVectorRouting.DV_ROUTE_REMOVAL_MS < curTime) {
				/*
				 * remove old entry, otherwise this will be selected as the best
				 * route when its seqNo becomes smaller than threshold used in
				 * seqNoGreater() function
				 *
				 * TODO: Instead of DV_ROUTE_REMOVAL_MS (2min) we should be able
				 * to calculate the right time based on DV_SEQNO_WINDOW!!!
				 * How???
				 */
				LOG.info("### timeout: removing " + rt);
				it.remove();
				if (rt == bestRoute) {
					bestRouteDown = true;
				}
			} else if (!rt.isDOWN() && rt.getLastTime() + DistanceVectorRouting.DV_ROUTE_DOWN_TIMEOUT_MS < curTime) {
				LOG.info("### timeout: change to DOWN " + rt);
				rt.stateDOWN();
				if (rt == bestRoute) {
					bestRouteDown = true;
				}
			}
		}
		if (bestRouteDown) {
			LOG.info("#### timeout: made bestRoute down");
			selectBestRoute();
		}

		/*
		 * timer based state changes for myself (DVPeer)
		 */

		if (state == State.DOWN_PENDING && curTime > downPendingExpireTime) {
			/* DOWN_PENDING probation time has expired */
			LOG.info("#### timeout: DOWN_PENDING probation time has expired -> DOWN");
			state = State.DOWN;
			downPendingExpireTime = 0;
			sendImmediateUpdateIfActiveFlow();
		}

		if (state != State.TIMED_OUT && lastTime + DistanceVectorRouting.PEER_TIMEOUT_MS < curTime) {
			/*
			 * We have not heard from this Peer in the timeout period, so we
			 * remove it and it's services from the registry.
			 * 
			 * But we keep it in our own list of Peers because we need to access
			 * some of its old state (seqNo) to avoid the count-to-infinity
			 * problem.
			 * 
			 * Note: PEER_TIMEOUT_MS is the time after we want this to happen,
			 * so it is the time period we allow Peers and its services to exist
			 * in the system even though all routes to it may be down.
			 */
			state = State.TIMED_OUT;
			LOG.info("#### timeout: " + this);
			peer.timeout();
			registry.removePeer(peer.deviceId);
		}
	}

	public boolean isNeighbour() {
		if (bestRoute != null && bestRoute.nextHop == this.deviceId) {
			return true;
		}
		return false;
	}

	public void setDownPendingExpireTimeLength(long x) {
		downPendingExpireTime = System.currentTimeMillis() + x;
	}

	public boolean isState(State state) {
		return this.state == state;
	}

	public boolean isFlag(Flag flag) {
		return this.flag == flag;
	}

	public void setFlag(Flag x) {
		flag = x;
	}

	public void markForImmediateUdate() {
		flag = Flag.SEND_IMMEDIATE;
		DVR.scheduleSendImmediate();
	}

	public void nbrRoutesDown(long nbr) {
		/*
		 * all routes which go through this nextNop node are considered to be
		 * broken now
		 *
		 * Note: We have to set the routes to DOWN instead of removing, in order
		 * to avoid selecting a new best route with a smaller seqNo. This is
		 * important for the distributed DV algorithm to work
		 */
		boolean bestRouteDown = false;
		for (Route rt : allRoutes.values()) {
			if (rt.nextHop == nbr) {
				rt.stateDOWN();
				if (rt == bestRoute) {
					bestRouteDown = true;
				}
			}
		}
		if (bestRouteDown) {
			LOG.info("#### NBR route down");
			selectBestRoute();
		}
	}

	public long getLastTime() {
		return lastTime;
	}

	public boolean hasRoutes() {
		return allRoutes.size() > 0;
	}

	public void addToSendQueue(ByteBuffer[] bufs, TransmissionType type, TrafficClass tos) {
		sendQueue.add(new SendItem(bufs, type, tos));
	}
}
