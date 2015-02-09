package com.thinktube.airtube.routing.nbr;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.thinktube.airtube.DataTransmit;
import com.thinktube.airtube.TrafficClass;
import com.thinktube.airtube.routing.RoutingModuleCommon;
import com.thinktube.net.NetworkInterfaces;
import com.thinktube.net.NetworkInterfaces.NetIf;
import com.thinktube.net.nio.PktConnection;

/**
 * Neighbour manager deals with link information only, which means Peers we have
 * a physical connection to, and the information about the links
 */
public class NeighbourManager implements DataTransmit.PacketHandler {
	private final static Logger LOG = Logger.getLogger(NeighbourManager.class.getSimpleName());
	public static final byte NBR_PKT_TYPE = 3;
	public static final int NBR_UPDATE_INTERVAL_MS = 1000;
	public static final int NBR_PROXY_UPDATE_INTERVAL_MS = 2000;
	protected static final long NBR_TIMEOUT_MS = 10000; // 10 sec
	private final RoutingModuleCommon router;
	private final long myDeviceId;
	private ConcurrentHashMap<Long, NbrPeer> nbrs = new ConcurrentHashMap<Long, NbrPeer>();
	short myLastSeq = 0;
	private long lastSendTime;
	private NetworkInterfaces nifs;

	public NeighbourManager(long deviceId, RoutingModuleCommon router) {
		this.myDeviceId = deviceId;
		this.router = router;
	}

	public void start(NetworkInterfaces nifs) {
		this.nifs = nifs;
	}

	private NbrPeer updateNbr(long fromDID, short pktSeqNo, short sharedSeqNo, InetAddress fromIP, PktConnection conn) {
		NbrPeer nbr = nbrs.get(fromDID);
		if (nbr == null) {
			nbr = new NbrPeer(fromDID);
			nbrs.put(fromDID, nbr);
			LOG.info("add " + nbr);
		}

		nbr.update(pktSeqNo, sharedSeqNo, fromIP, conn, nifs, router);
		return nbr;
	}

	private void updateReverseLink(NbrPeer nbr, InetAddress fromIP, short peerSeq, short mySeqNo, PktConnection conn) {
		NbrLink pl = nbr.updateReverseLink(fromIP, peerSeq, mySeqNo, conn);
		if (pl != null && pl.isBidirectional()) {
			if (!nbr.isOfficial) {
				//TODO: only add if the nbr has a bi-directional link too!
				makeNbrOfficial(nbr);
			}
		}
	}

	public void clear() {
		nbrs.clear();
	}

	public void timeout() {
		long time = System.currentTimeMillis();
		for (NbrPeer nb : nbrs.values()) {
			if (nb.getLastTime() + NBR_TIMEOUT_MS < time) {
				LOG.info("timeout " + nb);
				nbrs.remove(nb.deviceId);
				if (nb.isOfficial) {
					router.removeNbr(nb);
				}
				nb.timeout();
			}
		}
	}

	public NbrLink getBestLink(long deviceID) {
		NbrPeer np = nbrs.get(deviceID);
		if (np != null && np.isOfficial) {
			return np.getBestLink();
		}
		return null;
	}

	public void sendPacket(NetworkInterfaces nifs) {
		long curTime = System.currentTimeMillis();
		if (curTime < lastSendTime + NBR_UPDATE_INTERVAL_MS) {
			return;
		}

		LOG.fine("sending NBR (" + myLastSeq + ") after " + (System.currentTimeMillis() - lastSendTime) / 1000f + " sec");
		lastSendTime = curTime;

		/* build a different packet for each interface */
		for (NetIf ni : nifs.getInterfaces()) {
			/* no HELLO on mobile data (3G) interfaces */
			if (!ni.isMobile()) {
				ByteBuffer pkt = null;
				/* Lower HELLO frequency on proxy interfaces */
				if (ni instanceof ProxyLink) {
					ProxyLink pl = (ProxyLink)ni;
					if (pl.isSendTime()) {
						pkt = getPacket(ni, pl.getSeqNoAndIncrement());
					}
				} else {
					pkt = getPacket(ni, myLastSeq);
				}
				if (pkt != null) {
					DataTransmit.getInstance().sendBroadcastOnInterface(ni, pkt, TrafficClass.NORMAL);
				}
			}
		}

		myLastSeq++;
	}

	public ByteBuffer getPacket(NetIf ni, short nbrSeqNo) {
		LOG.fine("sending NBR (" + nbrSeqNo + ") on " + ni);
		ByteBuffer buf = ByteBuffer.allocate(1500);

		buf.put(NBR_PKT_TYPE);
		buf.putLong(myDeviceId);
		buf.putShort(router.getSeqNo());
		buf.putShort(nbrSeqNo);

		List<NbrLink> links = getLinksThru(ni);
		buf.putInt(links.size());
		for (NbrLink nl : links) {
			LOG.finer("  put " + nl);
			buf.putLong(nl.getNbr().deviceId);
			buf.putShort(nl.getLastSeq());
			//TODO: add bi-directional link flag
		}

		buf.flip();
		return buf;
	}

	private List<NbrLink> getLinksThru(NetIf ni) {
		List<NbrLink> links = new ArrayList<NbrLink>(nbrs.size());
		for (NbrPeer nb : nbrs.values()) {
			NbrLink nl = nb.getLinkThru(ni);
			if (nl != null) {
				links.add(nl);
			}
		}
		return links;
	}

	@Override
	public void receive(ByteBuffer buf, InetAddress fromIP, PktConnection conn) {
		long fromDID = buf.getLong();
		short sharedSeqNo = buf.getShort();
		short pktSeqNo = buf.getShort();

		if (fromDID == myDeviceId) {
			return;
		}

		LOG.fine("received NBR from " + Long.toHexString(fromDID) + " (" + pktSeqNo + ") on " + conn);

		NbrPeer fromNbr = updateNbr(fromDID, pktSeqNo, sharedSeqNo, fromIP, conn);

		int size = buf.getInt();
		for (int i = 0; i < size; i++) {
			long did = buf.getLong();
			short peerSeq = buf.getShort();

			LOG.finer("  read nbr [" + Long.toHexString(did) + "] (" + peerSeq + ")");
			if (did == myDeviceId) {
				// update reverse link quality but ignore info about ourself otherwise
				updateReverseLink(fromNbr, fromIP, peerSeq, myLastSeq, conn);
			} else {
				// ignore 2hop neighbour information
			}
		}
	}

	private void makeNbrOfficial(NbrPeer nbr) {
		if (nbr.isOfficial) {
			LOG.warning(nbr + " already is official?");
		}
		LOG.info(nbr + " became official");
		nbr.isOfficial = true;
		router.addNbr(nbr);
	}

	public boolean notifyDataReceptionFrom(long deviceId, InetAddress inetAddress, PktConnection conn) {
		NbrPeer nbr = nbrs.get(deviceId);
		if (nbr == null) {
			LOG.warning("received data from unknown nbr");
			return false;
		}

		NbrLink nl = nbr.updateReverseLink(inetAddress, conn);
		if (nl == null)
			return false;

		if (!nbr.isOfficial) {
			LOG.info(nbr + " got official by data reception");
			makeNbrOfficial(nbr);
		}
		return true;
	}

	public NbrPeer getNeighbour(long deviceId) {
		NbrPeer nbr = nbrs.get(deviceId);
		if (nbr != null && nbr.isOfficial) {
			return nbr;
		}
		return null;
	}
}
