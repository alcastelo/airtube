package com.thinktube.airtube;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.thinktube.net.NetworkInterfaces;
import com.thinktube.net.NetworkInterfaces.NetIf;
import com.thinktube.net.nio.NIOSelectorThread;
import com.thinktube.net.nio.PktConnectionCallback;
import com.thinktube.net.nio.PktConnection;
import com.thinktube.net.nio.UDPConnection;

public class DataTransmitUDPBroadcast implements DataTransmitI {
	private final static Logger LOG = Logger.getLogger(DataTransmitUDPBroadcast.class.getSimpleName());
	private static final int PORT = 9993;
	private final NIOSelectorThread selector;
	private final Map<InetAddress, PktConnection[]> bcastConns;
	private List<PktConnection> recvConns;

	public DataTransmitUDPBroadcast(NIOSelectorThread selector) {
		this.selector = selector;
		this.bcastConns = new ConcurrentHashMap<InetAddress, PktConnection[]>();
		this.recvConns = new ArrayList<PktConnection>();
	}

	@Override
	public void start(final PktConnectionCallback cb, NetworkInterfaces nifs) throws IOException {
		for (NetIf ia : nifs.getInterfaces()) {
			if (ia.hasBroadcast()) {
				LOG.info("BCAST on " + ia.getBroadcast() + " local " + ia.getAddress() + "/" + ia.getNetworkPrefixLength());

				/* receiver bound to one interface/broadcast address */
				UDPConnection ph = UDPConnection.createReceiver(selector, new InetSocketAddress(ia.getBroadcast(), PORT), cb);
				ph.setReceiveFilterIP(ia.getAddress());
				ph.setNetIf(ia);
				recvConns.add(ph);

				/* sender: create empty connection cache for each unique broadcast
				 * address, it will be filled in createConnection() */
				bcastConns.put(ia.getBroadcast(), new PktConnection[TrafficClass.values().length]);
			}
		}
	}

	@Override
	public void stop() {
		for (PktConnection ph : recvConns) {
			ph.close();
		}
		for (PktConnection[] ph : bcastConns.values()) {
			for (int i=0; i<TrafficClass.values().length; i++) {
				if (ph[i] != null) {
					LOG.info("close BCAST on " + ph[i]);
					ph[i].close();
					ph[i] = null;
				}
			}
		}
	}

	@Override
	public PktConnection createConnection(InetAddress bcast, TrafficClass tos, PktConnectionCallback cb) throws IOException {
		// cb is ignored here, since it's broadcast and receiving is done on a different connection
		PktConnection[] ph = bcastConns.get(bcast);
		if (ph[tos.ordinal()] == null) {
			LOG.info("create BCAST connection for " + bcast);
			ph[tos.ordinal()] = UDPConnection.createSender(selector, new InetSocketAddress(bcast, PORT), tos.getTOSValue(), true, null);
		}
		return ph[tos.ordinal()];
	}
}
