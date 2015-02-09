package com.thinktube.airtube.routing;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.logging.Logger;

import com.thinktube.airtube.AService;
import com.thinktube.airtube.AirTubeID;
import com.thinktube.airtube.Peer;
import com.thinktube.airtube.RemoteService;
import com.thinktube.airtube.ServiceDescription;

public class ServicePacketFmt {
	private final static Logger LOG = Logger.getLogger(ServicePacketFmt.class.getSimpleName());

	public static void writeServices(ByteBuffer buf, Collection<? extends AService> services) {
		// list of peers services
		buf.putInt(services.size());
		for (AService as : services) {
			LOG.fine("  put service " + as.id.port + " " + as.desc);
			buf.putShort(as.id.port);
			as.desc.toByteBuffer(buf);
		}
	}

	/**
	 * read services of a Peer from the packet
	 * 
	 * If peer is null the services are read from the packet, but ignored. This
	 * is useful to ignore updates based on seqNo, but nevertheless we still
	 * need to read and advance in the ByteBuffer packet in order to be at the
	 * right location for the next peer.
	 * 
	 * @param in
	 * @param peer
	 * @param peerSeqNo
	 * @return true if the services list has changed
	 */
	public static boolean readServices(ByteBuffer in, Peer peer, short peerSeqNo) {
		boolean servicesChanged = false;
		// list of peers services
		int size = in.getInt();
		for (int i = 0; i < size; i++) {
			short port = in.getShort();
			ServiceDescription desc = new ServiceDescription(in);
			if (peer != null) {
				AirTubeID id = new AirTubeID(port, peer.deviceId);
				servicesChanged = updateOrAddService(peer, id, desc, peerSeqNo);
			}
		}
		if (peer == null) {
			return false;
		}
		if (removeStaleServices(peer, peerSeqNo)) {
			servicesChanged = true;
		}
		return servicesChanged;
	}

	private static boolean updateOrAddService(Peer peer, AirTubeID id, ServiceDescription desc, short peerSeqNo) {
		boolean serviceAdded = false;
		LOG.fine("update service " + desc + " for " + peer + " (" + peerSeqNo + ")");
		RemoteService rs = peer.getService(id.port);
		if (rs == null) {
			LOG.info("add service " + id + " " + desc + " for " + peer);
			rs = new RemoteService(id, desc, peer);
			peer.addService(rs);
			serviceAdded = true;
		}
		rs.seqNo = peerSeqNo;
		return serviceAdded;
	}

	private static boolean removeStaleServices(Peer peer, int peerSeqNo) {
		boolean serviceRemoved = false;
		// remove services that once where announced (note we updated all services seqno before)
		for (RemoteService rs : peer.getServices()) {
			if (rs.seqNo != peerSeqNo) {
				// since the service does not exist remotely no need to unsubscribe,
				// just remove all subscriptions
				rs.removeAllClientSubscriptions();
				peer.removeService(rs);
				serviceRemoved = true;
				LOG.info("removed service " + rs + " for " + peer);
			}
		}
		return serviceRemoved;
	}
}
