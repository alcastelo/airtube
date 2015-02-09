package com.thinktube.airtube.routing.nbr;

import java.net.InetAddress;
import java.util.logging.Logger;

import com.thinktube.airtube.IpConnectionCache;
import com.thinktube.airtube.routing.Metric;
import com.thinktube.airtube.routing.MetricETTProducer;
import com.thinktube.net.NetworkInterfaces.NetIf;
import com.thinktube.net.nio.PktConnection;
import com.thinktube.net.nio.TCPConnection;
import com.thinktube.net.nio.UDPConnection;

public class NbrLink {
	private final static Logger LOG = Logger.getLogger(NbrLink.class.getSimpleName());
	protected NetIf netIf;
	private final InetAddress ip;
	protected NbrPeer nbr;
	protected final IpConnectionCache cc;
	private final MetricETTProducer metric = new MetricETTProducer();
	private long pktsReceived = 0;
	private long pktsReceivedRev = 0;
	private boolean isBidirectional;
	private short lastSeq;

	enum LinkType { PROXY, MOBILE, WIFI, ETH };
	private final LinkType type;

	// this is for the ProxyLink subclass
	protected NbrLink(InetAddress ip) {
		this.ip = ip;
		this.cc = new IpConnectionCache(ip);
		this.type = LinkType.PROXY;
		/*
		 * WARNING: nbr is null! But it will be set later, in NbrPeer.update()
		 * before we add the link to the NbrPeer, so it's safe
		 */
	}

	public NbrLink(NbrPeer pi, InetAddress ip, PktConnection conn) {
		this.nbr = pi;
		this.ip = ip;
		if (conn instanceof UDPConnection) {
			this.cc = new IpConnectionCache(ip);
			netIf = ((UDPConnection)conn).getNetIf();
			if (netIf == null) {
				throw new NullPointerException("NetIf should not be null!");
			} else if (netIf.isMobile()) {
				type = LinkType.MOBILE;
				metric.setLinkSpeed(1);
			} else if (netIf.isWireless()) {
				type = LinkType.WIFI;
				metric.setLinkSpeed(25);
			} else {
				type = LinkType.ETH;
				metric.setLinkSpeed(1000);
			}
		} else if (conn instanceof TCPConnection) {
			// proxy
			this.cc = new IpConnectionCache((TCPConnection)conn);
			this.type = LinkType.PROXY;
		} else {
			throw new RuntimeException("what's that connection?");
		}
	}

	@Override
	public String toString() {
		return "NbrLink [" + type + ip + " metric: " + metric + "(" + lastSeq + ")]";
	}

	public InetAddress getIp() {
		return ip;
	}

	public NbrPeer getNbr() {
		return nbr;
	}

	public void update(short seqNo) {
		int lost = 0;
		if (pktsReceived > 0 && seqNo > lastSeq + 1) {
			lost = seqNo - lastSeq - 1;
			LOG.fine("lost " + lost);
		}
		metric.updatePktLoss(lost);
		pktsReceived++;
		lastSeq = seqNo;
		LOG.fine("update " + this);
	}

	public void updateReverse(short peerSeq, short mySeq) {
		int lost = 0;
		if (pktsReceivedRev > 0 && mySeq > peerSeq + 1) {
			lost = mySeq - peerSeq - 1;
			LOG.fine("lost Rev " + (mySeq - peerSeq - 1));
		}
		metric.updatePktLossRev(lost);
		LOG.fine("update metric REV " + metric);
		pktsReceivedRev++;
		if (!isBidirectional) {
			LOG.info(this + " got bidirectional");
			isBidirectional = true;
		}
	}

	public IpConnectionCache getConnectionCache() {
		return cc;
	}

	public Metric getMetric() {
		return metric.getETT();
	}

	public boolean isBidirectional() {
		return isBidirectional;
	}

	public void setBidirectional(boolean b) {
		isBidirectional = b;
	}

	public short getLastSeq() {
		return lastSeq;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cc == null) ? 0 : cc.hashCode());
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
		NbrLink other = (NbrLink) obj;
		if (cc == null) {
			if (other.cc != null)
				return false;
		} else if (!cc.equals(other.cc))
			return false;
		return true;
	}
}
