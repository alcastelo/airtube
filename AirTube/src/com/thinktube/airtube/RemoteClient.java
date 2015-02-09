package com.thinktube.airtube;

public class RemoteClient extends AClient {
	private Peer peer;
	private DataTransmit sender;

	RemoteClient(AirTubeID id, Peer peer, ConfigParameters conf) {
		super(id, conf);
		this.peer = peer;
		this.sender = DataTransmit.getInstance();
	}

	@Override
	public void receiveData(AirTubeID from, ServiceData data, TransmissionType type, TrafficClass tos) {
		/* receive data from service - need to forward it to the real client.
		 * Note that this is normally called for unicast traffic, and NOT for BCAST service data,
		 * but it can be called with BCAST if the service wants to send to one client only */
		if (type == TransmissionType.UDP_BROADCAST) {
			ServiceDataPacket sdp = new ServiceDataPacket(from, this.id, data, type, tos);
			sender.sendBroadcast(sdp.toByteBuffers(), tos);
		} else {
			sender.send(new ServiceDataPacket(from, this.id, data, type, tos).toByteBuffers(), peer.deviceId, type, tos);
		}
	}

	@Override
	public String toString() {
		return "RemoteClient [" + id + "/" + peer + "]";
	}

	public void removeSubscription(AService as) {
		super.removeSubscription(as);
		/*
		 * remove remote client if it has no subscriptions left
		 * this is necessary because we create remote clients on subscription
		 * for local clients we rely on registerClient calls instead
		 */
		if (!isSubscribed()) {
			peer.removeClient(this);
		}
	}
}
