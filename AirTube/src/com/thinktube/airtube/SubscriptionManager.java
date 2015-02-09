package com.thinktube.airtube;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Logger;

import com.thinktube.net.nio.PktConnection;
import com.thinktube.util.ByteBufferUtil;

public class SubscriptionManager {
	private final static Logger LOG = Logger.getLogger(SubscriptionManager.class.getSimpleName());
	private final static long SUBSCRIPTION_RETRY_DELAY_MS = 5000; /* 5 sec */
	private final static int SUBSCRIPTION_RETRY_MAX = 12; /* give up after roughly one minute */
	private final AirTubeRegistry registry;
	private static SubscriptionManager instance;

	private HashSet<PendingSubscription> pendingSubs = new HashSet<PendingSubscription>();

	private static class PendingSubscription {
		long expiryTime;
		int nrOfRetries;
		final boolean isSubscribe; // or unsubscribe
		final AirTubeID serviceId;
		final AirTubeID clientId;
		final ServiceDataPacket pkt;

		PendingSubscription(AirTubeID serviceId, AirTubeID clientId, ServiceDataPacket pkt, boolean subs) {
			this.expiryTime = System.currentTimeMillis() + SUBSCRIPTION_RETRY_DELAY_MS;
			this.serviceId = serviceId;
			this.clientId = clientId;
			this.pkt = pkt;
			this.isSubscribe = subs;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((clientId == null) ? 0 : clientId.hashCode());
			result = prime * result
					+ ((serviceId == null) ? 0 : serviceId.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PendingSubscription other = (PendingSubscription) obj;
			if (clientId == null) {
				if (other.clientId != null)
					return false;
			} else if (!clientId.equals(other.clientId))
				return false;
			if (serviceId == null) {
				if (other.serviceId != null)
					return false;
			} else if (!serviceId.equals(other.serviceId))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return (!isSubscribe ? "UN" : "") + "SUBSCRIBE " + clientId.getString() + "->" + serviceId.getString() + " retries: " + nrOfRetries;
		}
	}

	public SubscriptionManager(AirTubeRegistry reg) {
		this.registry = reg;
		instance = this;
	}

	public static SubscriptionManager getInstance() {
		return instance;
	}

	public void receive(ServiceDataPacket sdp, Peer peer, PktConnection conn) {
		ByteBuffer buf = ByteBuffer.wrap(sdp.data.data);
		String line = ByteBufferUtil.getStringWOLength(buf);
		LOG.fine("TCP received from client: " + line);

		if (line.startsWith("SUBSCRIBE")) {
			RemoteClient rc = null;
			int sep = line.indexOf("CONF");
			AirTubeID sid = new AirTubeID(line.substring(10, sep -1));
			ConfigParameters conf = new ConfigParameters(line.substring(sep + 5, line.length()));

			LocalService ls = registry.getLocalService(sid);
			String s;
			if (ls != null) {
				rc = peer.getOrAddClient(sdp.srcId, conf);
				//TODO here the service could deny subscription
				s = "SUBSCRIPTION_RESULT 1";
			} else {
				LOG.warning("cannot subscribe to " + sid + ", not found");
				s = "SUBSCRIPTION_RESULT 0";
			}
			s = s + " CLIENT " + sdp.srcId.getString();

			/* send reply packet first, so the client can prepare */
			AirTubeID dst = new AirTubeID((short)0, sdp.srcId.deviceId);
			ServiceDataPacket sdp2 = new ServiceDataPacket(sid, dst, new ServiceData(s.getBytes(Charset.forName("UTF-8"))), TransmissionType.TCP, TrafficClass.NORMAL);
			try {
				conn.send(sdp2.toByteBuffers());
			} catch (ClosedChannelException e) {
				LOG.warning("subscription result could not be sent, closed");
			}

			/* now do the actual subscription */
			if (ls != null && rc != null) {
				ls.subscribe(rc);
			}
		}

		else if (line.startsWith("UNSUBSCRIBE")) {
			AirTubeID sid = new AirTubeID(line.substring(12, line.length()));

			LocalService ls = registry.getLocalService(sid);
			if (ls == null) {
				LOG.warning("service not found");
			} else {
				AClient ac = ls.getClient(sdp.srcId);
				if (ac == null) {
					LOG.warning("client not found");
				} else {
					ls.unsubscribe(ac);
				}
			}

			String s = "UNSUBSCRIPTION_RESULT 1 CLIENT " + sdp.srcId.getString();
			LOG.info("sending " + s);
			/* reply packet */
			AirTubeID dst = new AirTubeID((short)0, sdp.srcId.deviceId);
			ServiceDataPacket sdp2 = new ServiceDataPacket(sid, dst, new ServiceData(s.getBytes(Charset.forName("UTF-8"))), TransmissionType.TCP, TrafficClass.NORMAL);

			try {
				conn.send(sdp2.toByteBuffers());
			} catch (ClosedChannelException e) {
				LOG.warning("unsubscription result could not be sent, closed");
			}
		}

		else if (line.startsWith("SUBSCRIPTION_RESULT")) {
			LOG.info("SUBSCRIBE received reply..." + line);
			int sep2 = line.indexOf("CLIENT");
			short res = Short.parseShort(line.substring(20, sep2-1));
			AirTubeID cid = new AirTubeID(line.substring(sep2+7, line.length()));
			LOG.info("+++ cid " + cid);
			if (res == 0) {
				LOG.warning("subscription denied");
			} else {
				RemoteService rs = peer.getService(sdp.srcId.port);
				LocalClient lc = registry.getLocalClient(cid);
				rs.subscribeSuper(lc);
				LOG.info("subscription of " + lc + " to " + rs + " successful");
			}

			if (!removePendingSubscription(sdp.srcId, cid)) {
				LOG.warning("Received SUBSCRIPTION_RESULT for unknown request!");
			}
		}

		else if (line.startsWith("UNSUBSCRIPTION_RESULT")) {
			LOG.info("UNSUBSCRIBE received reply..." + line);
			int sep2 = line.indexOf("CLIENT");
			AirTubeID cid = new AirTubeID(line.substring(sep2+7, line.length()));
			LOG.info("+++ cid " + cid);

			if (!removePendingSubscription(sdp.srcId, cid)) {
				LOG.warning("Received UNSUBSCRIPTION_RESULT for unknown request!");
			}
		}

		else {
			LOG.warning("unknown command " + line);
		}
	}

	public void sendSubscribeToRemoteService(AirTubeID serviceId, AirTubeID clientId, ConfigParameters conf) {
		LOG.info("sending subscribe " + clientId + " to " + serviceId);

		String s = "SUBSCRIBE " + serviceId.getString() + " CONF " + conf.getString();
		AirTubeID dst = new AirTubeID(serviceId);
		dst.port = 0;
		ServiceDataPacket sdp = new ServiceDataPacket(clientId, dst, new ServiceData(s.getBytes(Charset.forName("UTF-8"))), TransmissionType.TCP, TrafficClass.NORMAL);

		pendingSubs.add(new PendingSubscription(serviceId, clientId, sdp, true));
		DataTransmit.getInstance().send(sdp.toByteBuffers(), serviceId.deviceId, TransmissionType.TCP, TrafficClass.NORMAL);
	}

	public void sendUnsubscribeToRemoteService(AirTubeID serviceId, AirTubeID clientId) {
		LOG.info("sending unsubscribe " + clientId + " to " + this);

		String s = "UNSUBSCRIBE " + serviceId.getString();
		AirTubeID dst = new AirTubeID(serviceId);
		dst.port = 0;
		ServiceDataPacket sdp = new ServiceDataPacket(clientId, dst, new ServiceData(s.getBytes(Charset.forName("UTF-8"))), TransmissionType.TCP, TrafficClass.NORMAL);

		pendingSubs.add(new PendingSubscription(serviceId, clientId, sdp, false));
		DataTransmit.getInstance().send(sdp.toByteBuffers(), serviceId.deviceId, TransmissionType.TCP, TrafficClass.NORMAL);
	}

	public void timeout() {
		long now = System.currentTimeMillis();
		Iterator<PendingSubscription> it = pendingSubs.iterator();
		while (it.hasNext()) {
			PendingSubscription sub = it.next();
			if (sub.expiryTime <= now) {
				if (sub.nrOfRetries < SUBSCRIPTION_RETRY_MAX) {
					sub.expiryTime = now + SUBSCRIPTION_RETRY_DELAY_MS;
					sub.nrOfRetries++;
					LOG.info("retry " + sub);
					DataTransmit.getInstance().send(sub.pkt.toByteBuffers(), sub.serviceId.deviceId, TransmissionType.TCP, TrafficClass.NORMAL);
				} else {
					LOG.warning("max retries reached for " + sub + " giving up");
					it.remove();
				}
			}
		}
	}

	/** Note that this does NOT remove pending UNsubscriptions! */
	public void removePendingSubscriptionsForClient(AirTubeID clientId) {
		Iterator<PendingSubscription> it = pendingSubs.iterator();
		while (it.hasNext()) {
			PendingSubscription sub = it.next();
			if (sub.clientId.equals(clientId) && sub.isSubscribe) {
				it.remove();
			}
		}
	}

	public void removePendingSubscriptionsForService(AirTubeID serviceId) {
		Iterator<PendingSubscription> it = pendingSubs.iterator();
		while (it.hasNext()) {
			PendingSubscription rec = it.next();
			if (rec.serviceId.equals(serviceId)) {
				it.remove();
			}
		}
	}

	private boolean removePendingSubscription(AirTubeID serviceId, AirTubeID clientId) {
		// PendingSubscription equals on serviceId and clientId
		PendingSubscription search = new PendingSubscription(serviceId, clientId, null, false);
		return pendingSubs.remove(search);
	}

	public boolean hasPendingSubscription(AirTubeID serviceId, AirTubeID clientId) {
		// PendingSubscription equals on serviceId and clientId
		PendingSubscription search = new PendingSubscription(serviceId, clientId, null, false);
		return pendingSubs.contains(search);
	}

	public void clear() {
		pendingSubs.clear();
	}
}
