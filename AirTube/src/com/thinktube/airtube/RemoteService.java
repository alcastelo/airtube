package com.thinktube.airtube;

import java.util.logging.Logger;

public class RemoteService extends AService {
	private final static Logger LOG = Logger.getLogger(RemoteService.class.getSimpleName());

	public Peer peer;
	public int seqNo;

	public RemoteService(AirTubeID id, ServiceDescription desc, Peer peer) {
		super(id, desc);
		this.peer = peer;
	}

	@Override
	public void receiveData(AirTubeID cid, ServiceData data) {
		/* receive data from client - need to send it to the real service */
		if (this.desc.type == TransmissionType.UDP_BROADCAST) {
			ServiceDataPacket sdp = new ServiceDataPacket(cid, this.id, data, desc.type, desc.tos);
			sender.sendBroadcast(sdp.toByteBuffers(), this.desc.tos);
		} else {
			sender.send(new ServiceDataPacket(cid, this.id, data, desc.type, desc.tos).toByteBuffers(), peer.deviceId, this.desc.type, this.desc.tos);
		}
	}

	@Override
	public String toString() {
		return "RemoteService [" + id + "]";
	}

	@Override
	public void subscribe(AClient ac) {
		if (!(ac instanceof LocalClient)) {
			LOG.severe("can only subscribe LocalCLient");
			return;
		}
		/*
		 * here we have to check the result from the remote service to
		 * know wether we really got subscribed
		 */
		SubscriptionManager.getInstance().sendSubscribeToRemoteService(this.id, ac.id, ac.config);
	}

	/* TODO this is needed since the subscription result is handled in SubscriptionManager now,
	 * and we can't access the superclass method from outside */
	public void subscribeSuper(AClient ac) {
		super.subscribe(ac);
	}

	@Override
	public void unsubscribe(AClient ac) {
		if (!(ac instanceof LocalClient)) {
			LOG.severe("can only unsubscribe LocalClient");
			return;
		}
		/*
		 * inform the remote service first to stop it sending data to us
		 * and then unconditionally remove the subscription
		 */
		SubscriptionManager.getInstance().sendUnsubscribeToRemoteService(this.id, ac.id);
		super.unsubscribe(ac);
	}
}
