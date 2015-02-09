package com.thinktube.airtube;

import java.net.InetAddress;
import java.util.logging.Logger;

import com.thinktube.net.nio.PktConnection;
import com.thinktube.net.nio.TCPConnection;

/**
 * An IP Address and a cache with open reusable connections to it.
 *
 * It also implements "proxy" functionality by allowing everything to be
 * "tunneled" over one TCP connection instead. If the proxy is set all other
 * connections will not be used
 */
public class IpConnectionCache {
	private final static Logger LOG = Logger.getLogger(IpConnectionCache.class.getSimpleName());
	private final InetAddress ip;
	private PktConnection conns[][];
	private TCPConnection proxyConn = null; // if this is set it will override all other connections!

	public IpConnectionCache(InetAddress to) {
		this.ip = to;
		// we need UDP and TCP handlers for all TrafficClasses
		this.conns = new PktConnection[2] [TrafficClass.values().length];
	}

	/** Construct with with proxy connection set */
	public IpConnectionCache(TCPConnection proxy) {
		this.ip = proxy.getRemoteIP();
		this.proxyConn = proxy;
	}

	public InetAddress getIP() {
		return ip;
	}

	public void setConnection(PktConnection conn, TransmissionType type, TrafficClass tos) {
		if (proxyConn == null) {
			conns[type.ordinal()][tos.ordinal()] = conn;
		}
	}

	public void setProxyConnection(TCPConnection proxy) {
		proxyConn = proxy;
	}

	public PktConnection getConnection(TransmissionType type, TrafficClass tos) {
		if (proxyConn != null) {
			return proxyConn;
		} else if (type != null && tos != null){
			return conns[type.ordinal()][tos.ordinal()];
		}
		return null;
	}

	public void closeConnections() {
		LOG.info("closing connections");
		if (proxyConn != null) {
			proxyConn.close();
		} else {
			for (int i=0; i < 2; i++) {
				for (int j=0; j < TrafficClass.values().length; j++) {
					if (conns[i][j] != null) {
						LOG.info("closing " + TransmissionType.values()[i] + " " + TrafficClass.values()[j]);
						conns[i][j].close();
						conns[i][j] = null;
					}
				}
			}
		}
	}

	@Override
	public String toString() {
		return "IpConnectionCache [ip=" + ip + "]" + (proxyConn != null ? " PROXY " + proxyConn : "");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ip == null) ? 0 : ip.hashCode());
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
		IpConnectionCache other = (IpConnectionCache) obj;
		if (ip == null) {
			if (other.ip != null)
				return false;
		} else if (!ip.equals(other.ip))
			return false;
		return true;
	}
}
