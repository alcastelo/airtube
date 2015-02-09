package com.thinktube.airtube.routing.nbr;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.thinktube.airtube.TrafficClass;
import com.thinktube.airtube.TransmissionType;
import com.thinktube.airtube.routing.RoutingModuleCommon;
import com.thinktube.net.NetworkInterfaces;
import com.thinktube.net.NetworkInterfaces.NetIf;
import com.thinktube.net.nio.PktConnection;
import com.thinktube.net.nio.TCPConnection;
import com.thinktube.net.nio.UDPConnection;

/**
 * Neighbour Peer, directly connected over a physical link.
 * 
 * Actually a "Neighbour" is not yet a full "Peer", just someone we hear on a
 * link. After it has passed bi-directional link check, it is made official and
 * the addNbr() method is called...
 */
public class NbrPeer {
	private final static Logger LOG = Logger.getLogger(NbrPeer.class.getSimpleName());
	public final long deviceId;
	private short lastSharedSeq;
	private long lastTime;
	private NbrLink bestLink;
	private Map<NetIf, NbrLink> links = new ConcurrentHashMap<NetIf, NbrLink>();
	public boolean isOfficial = false;

	public NbrPeer(long deviceId) {
		this.deviceId = deviceId;
	}

	@Override
	public String toString() {
		return "NbrPeer [" + Long.toHexString(deviceId) + " (" + lastSharedSeq + ")] ";
	}

	public NbrLink getBestLink() {
		return bestLink;
	}

	public void update(short seqNo, short sharedSeqNo, InetAddress fromIP, PktConnection conn, NetworkInterfaces nifs, RoutingModuleCommon router) {
		NbrLink nl = null;

		NetIf ni = conn.getNetIf();
		if (ni != null) {
			nl = links.get(ni);
		}

		// new direct link
		if (nl == null && conn instanceof UDPConnection) {
			nl = new NbrLink(this, fromIP, conn);
			links.put(ni, nl);
			LOG.info("New " + nl + " to " + this);
		}
		// new proxy link
		else if (nl == null && conn instanceof TCPConnection) {
			ProxyLink pl = new ProxyLink(this, fromIP, (TCPConnection)conn);
			if (nifs.addInterface(pl)) {
				LOG.info("Added downstream proxy interface " + pl);
			} else {
				/*
				 * Interface was already present which means we received the
				 * first HELLO from the other side of an "upstream" proxyLink,
				 * or a proxy "client" ("downstream") reconnected.
				 */
				ProxyLink oldPl = (ProxyLink)nifs.findInterface(pl);
				// Need to use the same TransmissionType and TrafficClass as DataTransmit does in sendBroadcastOnInterface() for ProxyLink!
				TCPConnection oldConn = (TCPConnection)oldPl.getConnectionCache().getConnection(TransmissionType.TCP, TrafficClass.VOICE);
				if (oldConn.equals(conn)) {
					/* Update upstream proxy */
					oldPl.setNbr(this);
					pl = oldPl;
					LOG.info("Updated upstream proxy interface " + pl);
				} else {
					/* Downstream proxy reconnected */
					nifs.removeInterface(oldPl);
					if (bestLink == oldPl) {
						bestLink = null;
					}
					nifs.addInterface(pl);
					LOG.info("Replaced downstream proxy interface with " + pl);
				}
			}
			links.put(pl, pl);
			conn.setNetIf(pl);
			nl = pl;
		}

		nl.update(seqNo);
		selectBestLink();

		short oldLastSharedSeq = lastSharedSeq;
		lastSharedSeq = sharedSeqNo;
		lastTime = System.currentTimeMillis();

		if (isOfficial && bestLink != null && router.seqNoGreater(sharedSeqNo, oldLastSharedSeq, 0, 0)) {
			router.updateNbr(this);
		}
	}

	private void selectBestLink() {
		NbrLink sel = null;
		for (NbrLink nl : links.values()) {
			if (nl.isBidirectional() && (sel == null || nl.getMetric().isBetterThan(sel.getMetric()))) {
				sel = nl;
			}
		}

		if (sel != bestLink) {
			LOG.info("best link changed from " + bestLink + " to " + sel);
			bestLink = sel;
		}
	}

	public void closeConnections() {
		LOG.info("closing connections");
		for (NbrLink pl : links.values()) {
			pl.getConnectionCache().closeConnections();
		}
	}

	public void timeout() {
		LOG.info("### timeout " + this);
		closeConnections();
		links.clear();
		bestLink = null;
		isOfficial = false;
	}

	/** update reverse link on normal NBR packet reception */
	public NbrLink updateReverseLink(InetAddress fromIP, short peerSeq, short mySeq, PktConnection conn) {
		NbrLink pl = links.get(conn.getNetIf());
		if (pl != null) {
			if (pl instanceof ProxyLink) {
				pl.updateReverse(peerSeq, ((ProxyLink)pl).getSeqNo());
			} else {
				pl.updateReverse(peerSeq, mySeq);
			}
			selectBestLink();
		} else {
			LOG.warning("reverse link update for unknown link? " + fromIP + " nbr " + this);
		}
		return pl;
	}

	/** update reverse on data reception */
	public NbrLink updateReverseLink(InetAddress fromIP, PktConnection conn) {
		NbrLink pl = links.get(conn.getNetIf());
		if (pl != null) {
			pl.setBidirectional(true);
			selectBestLink();
		} else {
			LOG.warning("reverse link update for unknown link? " + fromIP + " nbr " + this);
		}
		return pl;
	}

	public boolean hasBidirectionalLink() {
		for (NbrLink pl : links.values()) {
			if (pl.isBidirectional()) {
				return true;
			}
		}
		return false;
	}

	public NbrLink getLinkThru(NetIf ni) {
		return links.get(ni);
	}

	public long getLastTime() {
		return lastTime;
	}

	public short getLastSeq() {
		return lastSharedSeq;
	}

	public void removeLink(NbrLink link) {
		if (links.remove(link) == null) {
			LOG.warning(link + " to remove not found!");
		}
		if (bestLink != null && bestLink.equals(link)) {
			LOG.info("bestLink removed");
			selectBestLink();
		}
	}
}
