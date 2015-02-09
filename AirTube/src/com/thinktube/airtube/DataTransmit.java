package com.thinktube.airtube;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.thinktube.airtube.routing.AirTubeRouterI;
import com.thinktube.airtube.routing.nbr.ProxyLink;
import com.thinktube.net.NetworkInterfaces;
import com.thinktube.net.NetworkInterfaces.NetIf;
import com.thinktube.net.nio.NIOSelectorThread;
import com.thinktube.net.nio.PktConnection;
import com.thinktube.net.nio.PktConnectionCallback;

public class DataTransmit implements Runnable {
	private final static Logger LOG = Logger.getLogger(DataTransmit.class.getSimpleName());
	private final static int MAX_QUEUE = 100;
	private final static short MAX_PKT_TYPES = 10;
	private final static long DT_TIMEOUT_MS = 200;
	private final AirTubeRegistry registry;
	private final SubscriptionManager sm;
	private final AirTubeRouterI router;
	private final DataTransmitI trans[] = new DataTransmitI[TransmissionType.values().length];
	private static DataTransmit instance;
	private LinkedBlockingQueue<PacketInfo> recvQueue = new LinkedBlockingQueue<PacketInfo>(MAX_QUEUE);
	private PacketHandler[] pktHandlers = new PacketHandler[MAX_PKT_TYPES];
	private NetworkInterfaces nifs;
	private volatile boolean running;

	public interface PacketHandler {
		void receive(ByteBuffer buf, InetAddress from, PktConnection conn);
	}

	private static class PacketInfo {
		ByteBuffer buf;
		InetAddress from;
		PktConnection conn;
		PacketInfo(ByteBuffer buf, InetAddress from, PktConnection conn) {
			this.buf = ByteBuffer.allocate(buf.remaining());
			this.buf.put(buf);
			this.buf.flip();
			this.from = from;
			this.conn = conn;
		}
	}

	private final PktConnectionCallback receiveCb = new PktConnectionCallback() {
		@Override
		public void handleReceive(ByteBuffer buf, InetAddress from, PktConnection conn) {
			if (!recvQueue.offer(new PacketInfo(buf, from, conn))) {
				LOG.warning("receive queue full, pkt dropped");
			}
		}
	};

	@Override
	public void run() {
		PacketInfo pkt;
		long startTime;
		long sleepTime;

		LOG.info("DT running...");
		while (running) {
			try {
				/*
				 * Handle received packets.
				 * 
				 * Note that we do an "adaptive time" blocking here, to
				 *   1) handle received packets with minimum latency
				 *   2) allow this thread not to spin too fast (sleep)
				 *   3) guarantee that the below functions are called within
				 *      roughly every DT_TIMEOUT_MS.
				 * 
				 * It's important to do this because if we'd just always sleep
				 * for a fixed amount of time it can add up to > ~30secs spent
				 * in this loop which is an unacceptable amount of delay for
				 * sending.
				 */
				startTime = System.currentTimeMillis();
				sleepTime = DT_TIMEOUT_MS;
				while ((pkt = recvQueue.poll(sleepTime, TimeUnit.MILLISECONDS)) != null) {
					handleOnePacket(pkt.buf, pkt.from, pkt.conn);
					sleepTime = DT_TIMEOUT_MS - (System.currentTimeMillis() - startTime);
					if (sleepTime <= 0) {
						break;
					}
				}

				/*
				 * Give router a chance to send, but it's the router sub-modules
				 * decision if the actually want to send at this time. We call
				 * this timer with approximately DT_TIMEOUT_MS + processing delay
				 * from above.
				 */
				router.checkTimer();

				/* Retry subscriptions from SubscriptionManager */
				sm.timeout();

			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (Exception e) {
				LOG.warning("Caught exception!");
				e.printStackTrace();
			}
		}
		LOG.info("DT finished.");
	}

	private void handleOnePacket(ByteBuffer buf, InetAddress from, PktConnection conn) {
			/* check packet type and call it's handler */
			byte pktType = buf.get();
			if (pktType >= 0 && pktType < MAX_PKT_TYPES && pktHandlers[pktType] != null) {
				pktHandlers[pktType].receive(buf, from, conn);
			} else {
				LOG.warning("DROPPED unknown packet type " + pktType);
				return;
			}
	}

	private void handleDataPacket(ByteBuffer buf, InetAddress from, PktConnection conn) {
			ServiceDataPacket sdp = new ServiceDataPacket(buf);
			//LOG.info("*** received " + sdp + " from " + from);

			/* drop own packets, which we can receive with UDP_BROADCAST */
			if (sdp.srcId.deviceId == registry.deviceId) {
				return;
			}

			/* Broadcast */
			if (sdp.trans == TransmissionType.UDP_BROADCAST) {
				Peer peer = registry.getPeer(sdp.dstId.deviceId);
				if (peer == null) {
					LocalService ls = registry.getLocalService(sdp.dstId);
					if (ls != null) {
						ls.receiveData(sdp.srcId, sdp.data);
						return;
					}
					LocalClient lc = registry.getLocalClient(sdp.dstId);
					if (lc != null) {
						lc.receiveData(sdp.srcId, sdp.data, sdp.trans, sdp.tos);
					}
				} else {
					RemoteService rs = peer.getService(sdp.dstId.port);
					if (rs != null) {
						for (AClient ac : rs.getClients()) {
							ac.receiveData(sdp.srcId, sdp.data, rs.desc.type, rs.desc.tos);
						}
					}
				}
				return;
			}

			/* route if we are not the destination and if it's not BCAST */
			if (sdp.dstId.deviceId != registry.deviceId) {
				LOG.fine("forwarding " + sdp);
				send(sdp.toByteBuffers(), sdp.dstId.deviceId, sdp.trans, sdp.tos);
				return;
			}

			Peer peer = registry.getPeer(sdp.srcId.deviceId);
			if (peer == null) {
				LOG.warning("peer not found for " + Long.toHexString(sdp.srcId.deviceId));
				/*
				 * notify the routing / nbr module - it may decide to add the peer
				 *
				 * Note that a remote Nbr can decide a link is bidirectional,
				 * and immediately send a subscription request before we have
				 * received its HELLO packet and had a chance to make the link
				 * bidirectional. But when receiving its data traffic we can
				 * infer there is a bidirectional link. This code here handles
				 * this case.
				 */
				if (router.notifyDataReceptionFrom(sdp.srcId.deviceId, from, conn)) {
					// router successfully made peer available
					peer = registry.getPeer(sdp.srcId.deviceId);
					if (peer == null) {
						LOG.warning("peer still not found for " + Long.toHexString(sdp.srcId.deviceId));
						return;
					}
				} else {
					// router decided or was not able to make the peer official
					return;
				}
			}

			/* special port 0 is used for subscription manager */
			if (sdp.dstId.port == 0) {
				sm.receive(sdp, peer, conn);
				return;
			}

			/* service -> client */
			RemoteService rs = peer.getService(sdp.srcId.port);
			if (rs != null) {
				AClient ac = rs.getClient(sdp.dstId);
				if (ac == null) {
					if (!sm.hasPendingSubscription(rs.id, sdp.dstId)) {
						LOG.warning("client '" + sdp.dstId + "' of " + rs + " not found, sending unsubscribe!");
						sm.sendUnsubscribeToRemoteService(rs.id, sdp.dstId);
					} else {
						LOG.warning("*** received data before subscription result!");
					}
				} else {
					ac.receiveData(rs.id, sdp.data, rs.desc.type, rs.desc.tos);
				}
			} else {
				/* client -> service */
				LocalService ls = registry.getLocalService(sdp.dstId);
				if (ls == null) {
					LOG.warning("service '" + sdp.dstId + "' not found");
				} else {
					ls.receiveData(sdp.srcId, sdp.data);
				}
			}
			return;
		}

	public DataTransmit(NIOSelectorThread selector, final AirTubeRegistry services,
						final SubscriptionManager sm, AirTubeRouterI rout) {
		this.registry = services;
		this.sm = sm;
		this.router = rout;

		trans[TransmissionType.UDP.ordinal()] = new DataTransmitUDP(selector);
		trans[TransmissionType.UDP_BROADCAST.ordinal()] = new DataTransmitUDPBroadcast(selector);
		trans[TransmissionType.TCP.ordinal()] = new DataTransmitTCP(selector);

		instance = this;

		/* handle DATA packets here */
		this.setHandler(1, new PacketHandler() {
			@Override
			public void receive(ByteBuffer buf, InetAddress from, PktConnection conn) {
				handleDataPacket(buf, from, conn);
			}
		});
	}

	public static DataTransmit getInstance() {
		return instance;
	}

	public void setHandler(int type, PacketHandler handler) {
		if (type < MAX_PKT_TYPES) {
			pktHandlers[type] = handler;
		} else throw new RuntimeException("pktHandler index too high!");
	}

	public void start(NetworkInterfaces intf) throws IOException {
		this.nifs = intf;
		for (int i=0; i < trans.length; i++) {
			trans[i].start(receiveCb, intf);
		}
		running = true;
		new Thread(this, "DataTransmit Thread").start();
	}

	public void stop() {
		for (int i=0; i < trans.length; i++) {
			trans[i].stop();
		}
		running = false;
	}

	/*
	 * UNICAST
	 */

	/**
	 * Send method when we already have the IpConnectionCache
	 *
	 * @param bufs
	 *            The buffers you want to send. It's important that you "own"
	 *            (have allocated) the buffers you send, because the actual
	 *            sending will happen on a different thread. I.e. the ByteBuffer
	 *            you got in any receive callback
	 *            (PacketCallback.handleReceive()) can not be sent, as it is
	 *            allocated in the connection and will be reset and reused very
	 *            likely before the actual sending happens.
	 * @param ccDst
	 *            Destination
	 * @param type
	 *            Transmission Type
	 * @param tos
	 *            Type of service
	 */
	public void send(final ByteBuffer[] bufs, final IpConnectionCache ccDst, final TransmissionType type, final TrafficClass tos) throws ClosedChannelException {
		PktConnection conn = ccDst.getConnection(type, tos);
		if (conn == null) {
			try {
				conn = trans[type.ordinal()].createConnection(ccDst.getIP(), tos, receiveCb);
				ccDst.setConnection(conn, type, tos);
				LOG.info("=== send create " + type + " " + conn);
			} catch (IOException e) {
				LOG.warning("Could not create connection! Dropping packet!");
				e.printStackTrace();
				return;
			}
		}

		try {
			conn.send(bufs);
			LOG.fine("queued data in " + conn);
		} catch (ClosedChannelException e) {
			LOG.warning("Channel closed to [" + ccDst + "]");
			ccDst.setConnection(null, type, tos);
			throw e; // re-throw exception
		}
	}

	public void send(final ByteBuffer buf, final IpConnectionCache ccDst, final TransmissionType type, final TrafficClass tos) throws ClosedChannelException {
		send(new ByteBuffer[] { buf }, ccDst, type, tos);
	}

	/**
	 * Send method
	 *
	 * @param bufs
	 *            The buffers you want to send. It's important that you "own"
	 *            (have allocated) the buffers you send, because the actual
	 *            sending will happen on a different thread. I.e. the ByteBuffer
	 *            you got in any receive callback
	 *            (PacketCallback.handleReceive()) can not be sent, as it is
	 *            allocated in the connection and will be reset and reused very
	 *            likely before the actual sending happens.
	 * @param dstDID
	 *            Destination
	 * @param type
	 *            Transmission Type
	 * @param tos
	 *            Type of service
	 */
	public void send(final ByteBuffer[] bufs, final long dstDID, final TransmissionType type, final TrafficClass tos, boolean retry) {
		final IpConnectionCache dstCC = router.getNextHopCC(dstDID);
		if (dstCC == null) {
			LOG.info("No link to [" + Long.toHexString(dstDID) + "] found");
			if (retry) {
				LOG.info("queueing for retry to " + Long.toHexString(dstDID));
				router.queueToSendLater(bufs, dstDID, type, tos);
			}
			return;
		}

		try {
			send(bufs, dstCC, type, tos);
		} catch (ClosedChannelException e) {
			LOG.warning("Channel closed to [" + Long.toHexString(dstDID) + "]. Dropping packet!");
		}
	}

	/** convenience method for send when we only have one ByteBuffer */
	public void send(final ByteBuffer buf, final long dstDID, final TransmissionType type, final TrafficClass tos, boolean retry) {
		send(new ByteBuffer[] { buf }, dstDID, type, tos, retry);
	}

	/** default send method with default retry. Beware: VOICE and VIDEO is not retried! */
	public void send(final ByteBuffer[] bufs, final long dstDID, final TransmissionType type, final TrafficClass tos) {
		boolean retry = true;
		// We don't retry VOICE and VIDEO traffic
		if (tos == TrafficClass.VIDEO || tos == TrafficClass.VOICE) {
			retry = false;
		}
		send(bufs, dstDID, type, tos, retry);
	}

	/** convenience method for send when we only have one ByteBuffer, with default retry (Beware: VOICE and VIDEO is not retried!) */
	public void send(final ByteBuffer buf, final long dstDID, final TransmissionType type, final TrafficClass tos) {
		boolean retry = true;
		// We don't retry VOICE and VIDEO traffic
		if (tos == TrafficClass.VIDEO || tos == TrafficClass.VOICE) {
			retry = false;
		}
		send(new ByteBuffer[] { buf }, dstDID, type, tos, retry);
	}


	/*
	 * BROADCAST
	 */

	/** send a BCAST on one interface */
	public void sendOneBroadcast(final ByteBuffer[] bufs, final InetAddress bcastIP, final TrafficClass tos) {
		try {
			PktConnection conn = trans[TransmissionType.UDP_BROADCAST.ordinal()].createConnection(bcastIP, tos, null);
			conn.send(bufs);
		} catch (ClosedChannelException e) {
			LOG.info("+++ bcast channel closed");
		} catch (IOException e) {
			LOG.warning("Could not create BCAST connection! Dropping packet!");
			e.printStackTrace();
		}
	}

	/** send a BCAST on one interface, including PROXY interface where the broadcast is converted to unicast TCP */
	public void sendBroadcastOnInterface(NetIf ia, ByteBuffer[] bufs, TrafficClass tos) {
		if (ia instanceof ProxyLink) {
			try {
				DataTransmit.getInstance().send(bufs, ((ProxyLink)ia).getConnectionCache(), TransmissionType.TCP, TrafficClass.VOICE);
			} catch (ClosedChannelException e) {
				LOG.info("Channel closed on " + ia + ". Dropping packet and removing interface.");
				((ProxyLink)ia).close();
				nifs.removeInterface(ia);
			}
		} else if (ia.hasBroadcast()) {
			DataTransmit.getInstance().sendOneBroadcast(bufs, ia.getBroadcast(), tos);
		} else {
			LOG.info("NO BCAST on " + ia);
		}
	}

	/** send a BCAST on one interface, including PROXY interface where the broadcast is converted to unicast TCP */
	public void sendBroadcastOnInterface(NetIf ia, ByteBuffer buf, TrafficClass tos) {
		sendBroadcastOnInterface(ia, new ByteBuffer[] { buf }, tos);
	}

	/** send a BCAST on all interfaces, including PROXY interfaces where the broadcast is converted to unicast TCP */
	public void sendBroadcast(ByteBuffer[] bufs, TrafficClass tos) {
		LOG.fine("send BCAST on all interfaces");
		for (NetIf ia : nifs.getInterfaces()) {
			sendBroadcastOnInterface(ia, bufs, tos);
		}
	}

	/** send a BCAST on all interfaces, including PROXY interfaces where the broadcast is converted to unicast TCP */
	public void sendBroadcast(ByteBuffer buf, TrafficClass tos) {
		sendBroadcast(new ByteBuffer[] { buf }, tos);
	}
}
