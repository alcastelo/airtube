package com.thinktube.airtube.routing.nbr;

import java.net.InetAddress;
import java.net.NetworkInterface;

import com.thinktube.net.NetworkInterfaces.NetIf;
import com.thinktube.net.nio.TCPConnection;

public class ProxyLink extends NbrLink implements NetIf {
	//private final static Logger LOG = Logger.getLogger(ProxyLink.class.getSimpleName());
	private long lastSendTime = 0;
	private short mySeqNo = 0;

	/**
	 * This creates a "downstream" proxy interface/link, which is a "client"
	 * connecting to me and I am the proxy
	 */
	public ProxyLink(NbrPeer np, InetAddress ip, TCPConnection conn) {
		super(np, ip, conn);
		netIf = this;
	}

	/**
	 * This creates an "upstream" proxy interface/link, meaning that I connect
	 * to the proxy
	 */
	public ProxyLink(InetAddress proxyIp) {
		super(proxyIp);
		netIf = this;
	}

	/*** NetIf ***/

	@Override
	public InetAddress getAddress() {
		return cc.getIP();
	}

	@Override
	public InetAddress getBroadcast() {
		return  cc.getIP();
	}

	@Override
	public short getNetworkPrefixLength() {
		return 32;
	}

	@Override
	public String getName() {
		return "PROXY" + cc.getIP();
	}

	@Override
	public NetworkInterface getNetworkInterface() {
		return null;
	}

	@Override
	public String toString() {
		return "PROXY: " + cc.getConnection(null, null);
	}

	@Override
	public boolean hasBroadcast() {
		return false;
	}

	@Override
	public boolean isMobile() {
		return false;
	}

	@Override
	public boolean isWireless() {
		return false;
	}

	/*** Own ***/
	public void setNbr(NbrPeer nbrPeer) {
		nbr = nbrPeer;
	}

	public void close() {
		if (nbr != null)
			nbr.removeLink(this);
		if (cc != null)
			cc.closeConnections();
	}

	public boolean isSendTime() {
		long now = System.currentTimeMillis();
		if (now >= lastSendTime + NeighbourManager.NBR_PROXY_UPDATE_INTERVAL_MS) {
			lastSendTime = now;
			return true;
		}
		return false;
	}

	public short getSeqNo() {
		return mySeqNo;
	}

	public short getSeqNoAndIncrement() {
		return mySeqNo++;
	}
}
