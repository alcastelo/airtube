/* On-demand adhoc routing protocol complementing DV based routing protocol
 *
 * Usually, on-demand protocol does flooding search to find the route to the destination
 * the downside of on-demand approach is the delay and the overhead caused by the flooding
 * On the other hand, DV routing protocol has the downside of waiting for long time to receive
 * updates. This issue can be mitigated by shortening the time interval to send DV update
 * message but this is equivalent to flooding for all destination all the time
 *
 * Here, this on-demand protocol suppresses flooding overhead by referencing DV routing table
 * which has candidate alternative routes to the destination.
 */
package com.thinktube.airtube.routing.dv;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.thinktube.airtube.DataTransmit;
import com.thinktube.airtube.TrafficClass;
import com.thinktube.airtube.TransmissionType;
import com.thinktube.airtube.routing.Route;
import com.thinktube.airtube.routing.RoutingModuleCommon;
import com.thinktube.airtube.routing.nbr.NbrPeer;
import com.thinktube.airtube.routing.Metric;
import com.thinktube.airtube.routing.MetricETT;
import com.thinktube.net.nio.PktConnection;

public class OnDemandDV implements DataTransmit.PacketHandler {
	private final static Logger LOG = Logger.getLogger(OnDemandDV.class.getSimpleName());
	/* OD section id for multiple section format packet */
	public static final byte OD_PKT_TYPE = 5;
	private static final short TYPE_OD_REQUEST = 1;
	private static final short TYPE_OD_REPLY = 2;
	private static final int BCAST_TBL_EXPIRE_MS = 10000;
	private static final int SEND_RREQ_MIN_INTERVAL = 2000; /* 2 sec */
	private static final int SEND_RREQ_MAX_RETRY_CNT = 4;
	private static final int ROUND_TRIP_TIME_PER_HOP = 100; /* 100ms */
	private static final short SEND_RREQ_INIT_TTL = 3;
	private static final long ACTIVE_FLOW_EXPIRE_MS = 5000;	/* 5 sec */
	private static final int RREQ_FLAG_D = 0x00000001;		/* D flag: intermediate peer can not reply */
	private static final int AGE_DEFAULT_MS_FOR_OD_PROCESS = 1; /* 0ms or similar small value */

	private final RoutingModuleCommon router;
	private final DistanceVectorRouting dv;
	private final long myDeviceId;
	private int bcastId;

	private static class ReceiveRequestHistory {
		int bcastId;
		long timeStamp;

		public ReceiveRequestHistory() {
			timeStamp = 0;
			bcastId = 0;
		}
	}

	private static class SendRequestHistory {
		long deviceId;
		long lastSendTime;
		int ttlForRequest; /* ttl set in the last RREQ to this dst */
		int ttlForReply; /* metric from the dst to me. obtained from RREP */
		int ttlForRequestLastSuccess; /* ttl set in the last "successful" RREQ to this dst */
		boolean waitReply;
		int retryCnt;

		public SendRequestHistory(long deviceId) {
			/* constructor */
			this.deviceId = deviceId;
			waitReply = false;
			retryCnt = 0;
			lastSendTime = 0;
			ttlForRequest = 0;
			ttlForReply = 0;
			ttlForRequestLastSuccess = 0;
		}

		public void resetByRetryFailure() {
			waitReply = false;
			retryCnt = 0;
			ttlForRequest = 0;
		}

		public void update(long time, int ttl) {
			lastSendTime = time;
			ttlForRequest = ttl;
			waitReply = true;
		}

		public void suspendToSend(long time) {
			/* suspend to send operation until specified time */
			lastSendTime = time;
		}
	}

	private static class RelayNodeInfo {
		long deviceId;
		short seqNo;
		int age;
		Metric metric;

		public RelayNodeInfo(long deviceId, short seqNo, int age, Metric metric) {
			/* constructor */
			this.deviceId = deviceId;
			this.seqNo = seqNo;
			this.age = age;
			this.metric = metric;
		}
	}

	private static class ActiveFlow {
		long timeStamp;

		public ActiveFlow() {
		}

		public void update() {
			timeStamp = System.currentTimeMillis();
		}

		public boolean isAlive() {
			return (System.currentTimeMillis() < timeStamp
					+ ACTIVE_FLOW_EXPIRE_MS) ? true : false;
		}
	}

	private ConcurrentHashMap<Long, ReceiveRequestHistory> receiveRequestHistoryTbl = new ConcurrentHashMap<Long, ReceiveRequestHistory>();
	private ConcurrentHashMap<Long, SendRequestHistory> sendRequestHistoryTbl = new ConcurrentHashMap<Long, SendRequestHistory>();
	private ConcurrentHashMap<Long, ActiveFlow> activeFlowList = new ConcurrentHashMap<Long, ActiveFlow>();

	/* Constructor of OnDemandDV class */
	public OnDemandDV(long deviceId, RoutingModuleCommon router, DistanceVectorRouting dv) {
		this.router = router;
		this.dv = dv;
		this.myDeviceId = deviceId;
		bcastId = 0;
	}

	public boolean isActiveFlow(long dst) {
		ActiveFlow flow = activeFlowList.get(dst);
		if (flow != null && flow.isAlive()) {
			return true;
		} else {
			return false;
		}
	}

	public void updateActiveFlow(long dst) {
		LOG.finer("updateActiveFlow for " + Long.toHexString(dst));
		ActiveFlow flow = activeFlowList.get(dst);
		if (flow == null) {
			flow = new ActiveFlow();
			activeFlowList.put(dst, flow);
		}
		flow.update(); /* update its timeStamp */
	}

	public void timeout() {
		long curTime = System.currentTimeMillis();
		Route rt = null;

		/*
		 * retry RREQ again with larger ttl if we failed to receive the reply
		 * before the expected time
		 */
		for (SendRequestHistory srh : sendRequestHistoryTbl.values()) {

			if (srh.waitReply == true) {
				/* have sent RREQ recently and waiting for the reply */
				srh.retryCnt++; /* increment retry counter */

				LOG.fine("timeout sendRequestHistoryTbl DST[" + Long.toHexString(srh.deviceId) + "] retryCnt=" + srh.retryCnt + " elapseMS=" + (curTime - srh.lastSendTime) + " ttlForReq=" + srh.ttlForRequest);

				/*
				 * Fix Me later : srh.ttlForRequest does not show the real hops
				 * to search for UP route and NEED_TO_EXPLORE cases (ttl might
				 * not be decremented per hop )
				 */
				if (curTime > srh.lastSendTime + srh.ttlForRequest * ROUND_TRIP_TIME_PER_HOP) {

					/*
					 * if retry count reached to MAX without receiving the reply successfully
					 * then, stop sending RREQ for a certain length of period, otherwise
					 * send RREQ with larger ttl than previous time
					 */
					if (srh.retryCnt >= SEND_RREQ_MAX_RETRY_CNT) {
						 /* reset sendRequestHistory to this dst */
						srh.resetByRetryFailure();

						/*
						 * set different suspendToSend time depending on whether
						 * this dst was once existing or not so that we might
						 * avoid flooding search for non-existing peer
						 */
						rt = dv.getRoute(srh.deviceId);
						if (rt == null) {
							/*
							 * this dst might not exist in the network, we
							 * should avoid wide flooding in vain
							 */
							srh.suspendToSend(curTime + SEND_RREQ_MIN_INTERVAL * 5);
						} else {
							srh.suspendToSend(curTime + SEND_RREQ_MIN_INTERVAL * 2);
						}
						LOG.info("stop retrying RREQ DST[" + Long.toHexString(srh.deviceId) + "] retryCnt=" + srh.retryCnt + " reached to MAX");
					} else {
						/*
						 * we have not received the reply by the expected time,
						 * let's do another try with large ttl
						 */
						int flag = RREQ_FLAG_D;
						rt = dv.getRoute(srh.deviceId);
						/* increment up to real hop count measured by last RREP and expand the range by 2 hops */
						srh.ttlForRequest = (srh.ttlForRequest + 2 > srh.ttlForReply)? srh.ttlForRequest + 2 : srh.ttlForReply;
						if (rt != null) {
							sendRequest(srh.deviceId, rt.seqNo, rt.getCurrentAge(), srh.ttlForRequest, flag, srh.retryCnt);
						} else {
							/* seqNo = 0 and age = MAX  because we have no information to refer */
							sendRequest(srh.deviceId, (short) 0, Integer.MAX_VALUE, srh.ttlForRequest, flag, srh.retryCnt);
						}
						LOG.fine("retry RREQ DST[" + Long.toHexString(srh.deviceId) + "] retryCnt=" + srh.retryCnt);
					}
				}
			}
		}
	}

	public void routeExplore(long deviceId) {
		int flag = 0;
		int ttl = 0;
		short seqNo;
		int age;
		int retryCnt = 0;
		long curTime = System.currentTimeMillis();

		LOG.info("routeExplore for " + Long.toHexString(deviceId));

		SendRequestHistory srh = sendRequestHistoryTbl.get(deviceId);
		if (srh != null) {
			if (curTime < srh.lastSendTime + SEND_RREQ_MIN_INTERVAL) {
				/*
				 * do not start a new exploration within MIN_INTERVAL period to
				 * avoid incessant flooding of Request
				 */
				LOG.fine("routeExplore for " + Long.toHexString(deviceId) + " DROP [" +
						 (curTime - srh.lastSendTime) + " < " +  SEND_RREQ_MIN_INTERVAL + " elapsed]" );
				return;
			} else if (srh.waitReply == true) {
				/*
				 * skip this request because we are now exploring the route to the dst
				 */
				LOG.fine("routeExplore for " + Long.toHexString(deviceId) + " DROP [WAIT Reply is ON]" );
				return;
			}
		}

		DVPeer dp = dv.getDVPeer(deviceId);
		Route rt = null;
		if (dp != null) {
			rt = dp.getBestRoute();
		}

		/*
		 * TODO: we should consider to enable D flag later for some cases
		 */
		if (rt == null) {
			/*
			 * case 1: route entry does not exist. in this case, we have no
			 * seqNo record to refer
			 * 
			 * note: if this node exist in the network, DV should receive update
			 * message of any existing node in a certain period of time after
			 * the node joins the network
			 */
			seqNo = 0;
			age = Integer.MAX_VALUE;
			ttl = srh != null && srh.ttlForReply > 0 ? srh.ttlForReply + 1 : SEND_RREQ_INIT_TTL;
		} else if (rt.isDOWN()) {
			/* case 2: route is DOWN */
			seqNo = rt.seqNo;
			age = rt.getCurrentAge();
			ttl = srh != null && srh.ttlForRequestLastSuccess > 0 ? srh.ttlForRequestLastSuccess + 1 : SEND_RREQ_INIT_TTL;
		} else if (dp.isState(DVPeer.State.DOWN_PENDING)) {
			/*
			 * case 3: route is DOWN_PENDING
			 * 
			 * Alternate Disjoint route exist from me to the dst, but now we can
			 * not take this route as the best route because seqNo and Metric
			 * value does not meet the required condition to be selected as the
			 * best route
			 * 
			 * we need to explore the route to the dst now in order to get fresh
			 * seqNo (and metric) which gives me the best route with UP state
			 * 
			 * ttl = 1 should be good enough as the first try because the
			 * nexthop Nbr is believed to have UP state route to the dst
			 */
			seqNo = rt.seqNo;
			age = rt.getCurrentAge();
			ttl = 1;
		} else {
			/*
			 * case 4: route is UP
			 * 
			 * TODO: Usually we do not need to do explore for UP route.
			 */
			seqNo = rt.seqNo;
			age = rt.getCurrentAge();
			ttl = 2;
			//return;
		}
		flag = RREQ_FLAG_D;
		sendRequest(deviceId, seqNo, age, ttl, flag, retryCnt);

		/* modify the probation time length of DOWN_PENDING state */
		if (dp != null && dp.isState(DVPeer.State.DOWN_PENDING)) {
			dp.setDownPendingExpireTimeLength((long) 3 * ttl * ROUND_TRIP_TIME_PER_HOP);
		}
	}

	private void sendRequest(long dstDeviceId, short dstSeqNo, int dstAge, int ttl, int flag, int retryCnt) {
		ByteBuffer buf = ByteBuffer.allocate(1500);
		long curTime = System.currentTimeMillis();

		LOG.info("send RREQ DST[" + Long.toHexString(dstDeviceId) + "] dstSeqNo[" + dstSeqNo + "] flag[" + flag + "]");

		router.incrementSeqNo(); /* increment my seqNo */
		bcastId++;          /* increment my bcastId */

		/* create packet */
		buf.put(OD_PKT_TYPE);
		buf.putShort(TYPE_OD_REQUEST);
		buf.putInt(bcastId);
		buf.putLong(dstDeviceId);
		buf.putShort(dstSeqNo);
		buf.putInt(dstAge);
		buf.putLong(myDeviceId);
		buf.putShort(router.getSeqNo());
		buf.putShort((short) ttl);
		buf.putInt(flag);
		buf.putInt(0); /* number of relay node record */

		buf.flip();
		DataTransmit.getInstance().sendBroadcast(buf, TrafficClass.VOICE);

		/* update send request history. necessary to do retry */
		SendRequestHistory srh = sendRequestHistoryTbl.get(dstDeviceId);
		if (srh == null) {
			srh = new SendRequestHistory(dstDeviceId);
			sendRequestHistoryTbl.put(dstDeviceId, srh);
		}
		srh.update(curTime, ttl);
	}

	private void sendReply(ByteBuffer out, long src, short srcSeqNo, long dst, short dstSeqNo, int dstAge, int flag) {
		/*
		 * FIXME later: should not increment seqNo for second or later reply
		 * with the same bcastId request
		 */
		router.incrementSeqNo();

		ByteBuffer buf = buildReplyMessage(out, src, srcSeqNo, dst, dstSeqNo, dstAge, flag);

		LOG.info("send RREP SRC[" + Long.toHexString(src) + "] DST[" + Long.toHexString(dst) + "]");

		DataTransmit.getInstance().send(buf, src, TransmissionType.UDP, TrafficClass.VOICE, false);
	}

	@Override
	public void receive(ByteBuffer in, InetAddress from, PktConnection unused_conn) {
		switch (in.getShort()) {
		case TYPE_OD_REQUEST:
			receiveRequest(in);
			break;
		case TYPE_OD_REPLY:
			receiveReply(in);
			break;
		}
	}

	private void receiveRequest(ByteBuffer in) {
		long curTime = System.currentTimeMillis();
		NbrPeer fromNbr = null;

		int bcastId = in.getInt();
		long dst = in.getLong();
		short dstSeqNo = in.getShort();
		int dstAge = in.getInt();
		long src = in.getLong();
		short srcSeqNo = in.getShort();
		short ttl = in.getShort();
		int flag = in.getInt();
		int position_nodeNum = in.position();

		/*
		 * Verify RREQ to judge whether it should be dropped or not
		 * we should drop this RREQ if
		 * - RREQ originator is myself
		 * - already received this RREQ once before
		 */
		if (src == myDeviceId) {
			LOG.fine("DROP RREQ reason[SRC is myself] DST[" + Long.toHexString(dst) + "]");
			return;
		}

		ReceiveRequestHistory record = receiveRequestHistoryTbl.get(src);
		if (record == null) {
			record = new ReceiveRequestHistory();
			receiveRequestHistoryTbl.put(src, record);
		}

		if (bcastId <= record.bcastId && curTime < record.timeStamp + BCAST_TBL_EXPIRE_MS) {
			/* we have already received this once before, so drop it */
			LOG.fine("DROP RREQ [already received before] DST[" + Long.toHexString(dst) + "]");
			return;
		}

		/*
		 * this RREQ has not been received before, then accept and record this
		 * in the history table
		 */
		record.bcastId = bcastId;
		record.timeStamp = curTime;

		LOG.fine("receive RREQ SRC[" + Long.toHexString(src) + "] DST[" + Long.toHexString(dst) + "] dstSeqNo="
				 + dstSeqNo + " bcastId=" + bcastId + " ttl=" + ttl + " flag=" + flag);

		/* read relay node list into reverse order list */
		ArrayList<RelayNodeInfo> nodeList = readRelayNodes(in, src, srcSeqNo, 0);

		/* routing loop detected */
		if (nodeList == null) {
			LOG.fine("DROP RREQ reason[Forwarded once before from myself] DST[" + Long.toHexString(dst) + "]");
			return;
		}

		fromNbr = updateRelayNodesRoutes(nodeList);
		if (fromNbr == null) {
			LOG.warning("DROP RREQ reason[Nbr unknown] DST[" + Long.toHexString(dst) + "]");
			return;
		}

		/*
		 * drop(?) if the requester is my predecessor to the dst
		 * 
		 * this can be an implicit notification the route to the dst was broken
		 * or can be just exploring backup route...
		 * 
		 * we should not drop here if we do flooding search because one of my
		 * successor might have alternate route to the dst which does not pass
		 * through myself
		 */

		if (dst == myDeviceId) {
			/*
			 * case 1: I am the dst node, then I should reply
			 */
			LOG.info("receive RREQ [I am the dst of RREQ] DST[" + Long.toHexString(dst) + "]");
			sendReply(in, src, srcSeqNo, myDeviceId, router.getSeqNo(), 0, 0);  /* age = 0, flag = 0 */
		} else {
			Route rt2dst = dv.getRoute(dst);
			LOG.fine("found " + rt2dst);
			if ((flag & RREQ_FLAG_D) == 0 && rt2dst != null && rt2dst.isUP() &&
					router.seqNoGreater(rt2dst.seqNo, dstSeqNo, rt2dst.getCurrentAge(), dstAge)) {
				/*
				 * case 2: I have fresher route record, then I will reply as a proxy
				 * if D flag is on, we should not reply on behalf of the dst
				 */
				LOG.info("reply as proxy for DST[" + Long.toHexString(dst) + "] seqNo[" + rt2dst.seqNo + "]");
				sendReply(in, src, srcSeqNo, dst, rt2dst.seqNo, rt2dst.getCurrentAge(), 0);
			} else {
				/*
				 * case 3: forward request if (ttl - 1 > 0) if
				 * - valid route to this dst does not exist in my DV routing table or
				 * - valid route to this dst exit in my DV routing table, but that is NOT
				 *   fresher than the one which the requester (RREQ sender) has.
				 * - RREQ_FLAG_D is ON  (Don't reply except the final dst node)
				 */

				/* before forwarding, let's decrement ttl except for the case below
				 *  (1) valid route to the dst exist and its state is UP in my DV routing table AND
				 *  (2) the requester (RREQ sender) is not an upstream node staying on the route
				 *      from me to the dst node.
				 *
				 * therefore, we decrement ttl for the following cases.
				 * - valid route to the dst does not exist or
				 * - route is DOWN or
				 * - route is UP and the requester is located in the upstream
				 *   towards the dst
				 */
				if (rt2dst == null || rt2dst.isDOWN()
						|| (rt2dst.isUP() && dv.isUpstreamPeer(src, rt2dst.deviceId))) {
					ttl--;
				}

				in.flip();

				if (ttl > 0) {
					if (rt2dst == null || rt2dst.isDOWN()) {
						LOG.info("forward RREQ [valid route NOT available] DST[" + Long.toHexString(dst) + "] ttl=" + ttl);
					} else if ((flag & RREQ_FLAG_D) == 0) {
						LOG.info("forward RREQ [RREQ_FLAG_D is ON] DST[" + Long.toHexString(dst) + "] ttl=" + ttl);
					} else {
						LOG.info("forward RREQ [fresher route NOT available] DST[" + Long.toHexString(dst) + "] ttl=" + ttl);
					}
					Metric metric = fromNbr.getBestLink().getMetric();
					forwardRequest(in, src, dst, metric, position_nodeNum);
				} else {
					LOG.info("stop forwarding RREQ [ttl decremented to 0] DST[" + Long.toHexString(dst) + "]");
				}
			}
		}
	}

	private ArrayList<RelayNodeInfo> readRelayNodes(ByteBuffer in, long src, short srcSeqNo, int srcAge) {
		/*
		 * Build list of relay node info, in reverse order.
		 *
		 * If the recorded relay route was {SRC -> A -> B -> C -> MyNode} we
		 * build a list of RelayNodeInfo which is ordered by {C, B, A, SRC} and
		 * index [0] C has the smallest metric and is our next hop towards SRC
		 * and must be my Nbr
		 */
		int nodeNum = in.getInt();
		ArrayList<RelayNodeInfo> nodeList = new ArrayList<RelayNodeInfo>(nodeNum + 1);

		long relayId;
		short relaySeqNo;
		int age;
		Metric metric;

		/* add SRC/DST */
		/* dstAge is not 0 when an intermediate(relay) node generates this RREP */
		age = srcAge > 0 ? srcAge : AGE_DEFAULT_MS_FOR_OD_PROCESS;
		RelayNodeInfo nodeInfo = new RelayNodeInfo(src, srcSeqNo, age, null);
		nodeList.add(0, nodeInfo);

		/* add A, B, C at the HEAD position of the list */
		for (int i = 0; i < nodeNum; i++) {
			relayId = in.getLong();
			/*
			 * drop this message if this was forwarded once before from myself
			 * note: this should not happen if bcastId works correctly but is a
			 * good safety net in case of routing loops
			 */
			if (relayId == myDeviceId) {
				return null;
			}
			relaySeqNo = in.getShort();
			age = in.getInt();
			metric = MetricETT.readFrom(in);
			LOG.fine("read relay node list loop [" + i + "] nodeNum=" + nodeNum +
					" relayId[" + Long.toHexString(relayId) + "]" +
					" seq[" + relaySeqNo + "]" +
					" age[" + age + "]" +
					" metric[" + metric + "]");
			nodeInfo = new RelayNodeInfo(relayId, relaySeqNo, age, metric);
			nodeList.add(0, nodeInfo);
		}
		return nodeList;
	}

	private NbrPeer updateRelayNodesRoutes(ArrayList<RelayNodeInfo> nodeList) {
		/*
		 * update reverse (RREQ) or forward (RREP) route
		 *
		 * nodeList has {C, B, A, SRC} in that order for the route
		 * {SRC -> A -> B -> C -> MyNode - - - > DST}
		 *
		 * The first entry C is my nbr
		 *
		 * Note that even though the list is reversed, the entry has the metric
		 * from the original direction:
		 *   SRC: metric "null"
		 *     A: metric between SRC -> A
		 *     B: metric between A -> B
		 *     C: metric between B -> C
		 */
		long relayLastHop = myDeviceId;
		Metric metricAccum = new MetricETT(0.0);

		NbrPeer fromNbr = router.getNeighbour(nodeList.get(0).deviceId);
		if (fromNbr == null) {
			return null;
		}

		/*
		 * Caution: don't add the metric to my nbr, which is added within
		 * DVPeer::updateRoute()
		 *
		 * Still we need to update Nbr routes as well to stay consistent
		 * concerning seqNos within the whole DV network
		 */
		for (RelayNodeInfo ni : nodeList) {
			LOG.fine("  update route to " +
					Long.toHexString(ni.deviceId) +
					" metric=" + metricAccum +
					" seq=" + ni.seqNo +
					" age=" + ni.age +
					" nh[" + Long.toHexString(fromNbr.deviceId) + "]" +
					" lh[" + Long.toHexString(relayLastHop) + "]");

			dv.updatePeer(ni.deviceId, ni.seqNo, metricAccum, fromNbr, relayLastHop, ni.age);

			relayLastHop = ni.deviceId;
			metricAccum.add(ni.metric);
		}

		return fromNbr;
	}

	private void forwardRequest(ByteBuffer out, long src, long dst, Metric metric, int position_nodeNum) {
		/* add my node info to the relay node list and send broadcast */
		LOG.info("forward RREQ SRC[" + Long.toHexString(src) + "] DST[" + Long.toHexString(dst) + "]");

		router.incrementSeqNo(); /* increment my seqNo */
		ByteBuffer buf = addMyselfToRelayNodes(out, position_nodeNum, metric);

		DataTransmit.getInstance().sendBroadcast(buf, TrafficClass.VOICE);
	}

	private void receiveReply(ByteBuffer in) {
		/*
		 * if the request originator is me, then just update my routing tbl, no
		 * need to forward
		 *
		 * otherwise update my routing table and keep forwarding to the original
		 * requester
		 */

		in.getInt();					/* bcastId: not used right now */
		long src = in.getLong();		/* src: deviceId which originates the Request message */
		in.getShort();					/* srcSeqNo: not used right now */
		long dst = in.getLong();		/* dst: deviceId, to which the route is explored */
		short dstSeqNo = in.getShort(); /* dstSeqNo: new seqNo from the dst or intermediary */
		int dstAge = in.getInt();
		short ttl = in.getShort();		/* ttl */
		in.getInt();					/* flag: not used */
		int position_nodeNum = in.position();

		LOG.fine("receive RREP SRC[" + Long.toHexString(src) + "] DST[" + Long.toHexString(dst) + "]");

		ArrayList<RelayNodeInfo> nodeList = readRelayNodes(in, dst, dstSeqNo, dstAge);

		/* routing loop detected */
		if (nodeList == null) {
			LOG.fine("DROP RREP reason[Forwarded once before from myself] DST[" + Long.toHexString(dst) + "]");
			return;
		}

		NbrPeer fromNbr = updateRelayNodesRoutes(nodeList);
		if (fromNbr == null) {
			LOG.warning("DROP RREP reason[Nbr unknown] DST[" + Long.toHexString(dst) + "]");
			return;
		}

		int dstHopCnt = nodeList.size();

		if (src == myDeviceId) {
			/*
			 * I am the originator of this RREQ
			 * we need to update sendRequestHistory
			 */
			SendRequestHistory srh = sendRequestHistoryTbl.get(dst);
			if (srh == null) {
				srh = new SendRequestHistory(dst);
				sendRequestHistoryTbl.put(dst, srh);
			}

			/*
			 * reset RREQ history
			 * record the ttl of success request for later reference
			 */
			srh.waitReply = false;
			srh.retryCnt = 0;
			srh.ttlForReply = dstHopCnt;
			srh.ttlForRequestLastSuccess = srh.ttlForRequest - ttl ;
		} else {
			/* I am NOT the original requester, need to forward */
			in.flip();
			Metric metric = fromNbr.getBestLink().getMetric();
			forwardReply(in, src, dst, metric, position_nodeNum);
		}
	}

	private void forwardReply(ByteBuffer out, long src, long dst, Metric metric, int position_nodeNum) {
		/*
		 * add my node info to the relay node list and send unicast toward the
		 * downstream
		 */
		LOG.info("forward RREP SRC[" + Long.toHexString(src) + "] DST[" + Long.toHexString(dst) + "]");

		router.incrementSeqNo(); /* increment my seqNo */
		ByteBuffer buf = addMyselfToRelayNodes(out, position_nodeNum, metric);

		DataTransmit.getInstance().send(buf, src, TransmissionType.UDP, TrafficClass.VOICE, false);
	}

	private ByteBuffer addMyselfToRelayNodes(ByteBuffer out, int position_nodeNum, Metric metric) {
		/* allocate buffer to accommodate additional fields */
		ByteBuffer buf = ByteBuffer.allocate(out.limit() + 14 + metric.getDataSize());

		/* put original contents */
		buf.put(out);

		/* add my relay node info at the tail */
		buf.putLong(myDeviceId);
		buf.putShort(router.getSeqNo());
		buf.putInt(AGE_DEFAULT_MS_FOR_OD_PROCESS); /* age */
		metric.writeTo(buf);

		/* update nodeNum field by incrementing by 1 */
		int nodeNum = buf.getInt(position_nodeNum);
		buf.putInt(position_nodeNum, nodeNum + 1);

		buf.flip();
		return buf;
	}

	/**
	 *
	 * @param out
	 * @param src
	 *            deviceId which originates the Request message
	 * @param srcSeqNo
	 * @param dst
	 *            deviceId, to which the route is explored
	 * @param dstSeqNo
	 * @param flag
	 * @return
	 */
	private ByteBuffer buildReplyMessage(ByteBuffer out, long src, short srcSeqNo, long dst, short dstSeqNo, int dstAge, int flag) {
		ByteBuffer buf = ByteBuffer.allocate(1200);
		int num = 0;
		LOG.info("building RREP message for dst[" + Long.toHexString(dst) + "] src[" + Long.toHexString(src) + "]");

		/* create reply message */
		buf.put(OD_PKT_TYPE);
		buf.putShort(TYPE_OD_REPLY);
		buf.putInt(0);				/* bcastId : we do not need to specify bcastId for Reply */
		buf.putLong(src);
		buf.putShort(srcSeqNo);
		buf.putLong(dst);
		buf.putShort(dstSeqNo);
		buf.putInt(dstAge);
		buf.putShort((short) 1);	/* ttl: Reply is unicast, then we do not need "ttl" for Reply */
		buf.putInt(flag);
		int position_nodeNum = buf.position();
		buf.putInt(0);				/* relay node num */

		/*
		 * we need to take care of the two different cases below:
		 */
		if (dst == myDeviceId) {
			/*
			 * case 1: I am the final dst node, nothing to be added here
			 */
		} else {
			/*
			 * case 2: I am NOT the final dst node, but am the intermediate node who
			 * has a fresher route to the dst
			 * 
			 * need to put the relay nodes from the dst up to myself (excl the final dst)
			 *
			 * SRC - A - B - C - MyNode - D - E - DST, then we need to add D and E in here
			 */
			Route rt_back = dv.getRoute(dst);
			Route rt = dv.getRoute(rt_back.lastHop);
			while (rt != null) {
				buf.putLong(rt.deviceId);
				buf.putShort(rt.seqNo);
				buf.putInt(rt.getCurrentAge());

				/*  write the metric for each hop */
				Metric metricTmp = rt_back.metric.copy();
				metricTmp.subtract(rt.metric);  /* rt_back.metric - rt.metric   */
				metricTmp.writeTo(buf);

				LOG.fine("building RREP message for dst[" + Long.toHexString(dst) + "] src["
						+ Long.toHexString(src) + "]" + " relay[" + num + "]" + " relayId=" + Long.toHexString(rt.deviceId)
						+ " lastHop=" + Long.toHexString(rt.lastHop));

				rt_back = rt;
				rt = dv.getRoute(rt.lastHop); /* rt is null if lasthop is myself */
				num++;
			}
			/* add the relay node info of myself node */
			buf.putLong(myDeviceId);
			buf.putShort(router.getSeqNo());
			buf.putInt(AGE_DEFAULT_MS_FOR_OD_PROCESS);  /* age of my own information */
			rt_back.metric.writeTo(buf);  /* metric between nextHop (D) and MyNode */

			/* update relay node num field */
			num++;
			buf.putInt(position_nodeNum, num);
		}

		buf.flip();
		return buf;
	}
}
