package com.thinktube.airtube;

import java.net.InetAddress;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class Peers {
	protected static final long PEER_TIMER_MS = 10000; // 10 sec
	protected static final long PEER_TIMEOUT_MS = 60000; // 60 sec

	Timer timer;
	public Map<InetAddress, Peer> peers = new ConcurrentHashMap<InetAddress, Peer>();

	public Peers() {
	}
	
	public void start() {
		timer = new Timer("Peer timeout", true);
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				long time = System.currentTimeMillis();
				for (Peer p : peers.values()) {
					if (p.lastTime + PEER_TIMEOUT_MS < time) {
						peers.remove(p.ip);
						// if (peerCb != null)
						// peerCb.peerTimeout(p);
					}
				}
			}
		}, PEER_TIMER_MS, PEER_TIMER_MS);
	}
	
	public void stop() {
		timer.cancel();
		timer.purge();
	}

	public Peer addOrUpdate(InetAddress addr, int seq) {
		Peer peer = peers.get(addr);

		if (peer == null) {
			// add new peer
			peer = new Peer(addr, seq, System.currentTimeMillis());
			peers.put(addr, peer);
			// sa.callback(peer);
		} else {
			// update existing peer
			peer.lastTime = System.currentTimeMillis();
			if (peer.lastSeq > seq) { // peer restarted
				// sa.callback(peer);
			}
		}
		return peer;
	}

	public Peer get(InetAddress inetAddress) {
		return peers.get(inetAddress);
	}
}
