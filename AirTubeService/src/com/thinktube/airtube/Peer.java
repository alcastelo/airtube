package com.thinktube.airtube;

import java.net.InetAddress;

public class Peer {
	public InetAddress ip;
	public int lastSeq;
	public long lastTime;

	public Peer(InetAddress ip, int lastSeq, long lastTime) {
		this.ip = ip;
		this.lastSeq = lastSeq;
		this.lastTime = lastTime;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Peer other = (Peer) obj;
		if (ip == null) {
			if (other.ip != null)
				return false;
		} else if (!ip.equals(other.ip))
			return false;
		return true;
	}
}