package com.thinktube.airtube;

public class LocalService extends AService {
	private AirTubeCallbackI cb;

	LocalService(AirTubeID id, ServiceDescription desc, AirTubeCallbackI cb) {
		super(id, desc);
		this.cb = cb;
	}

	@Override
	public void receiveData(AirTubeID cid, ServiceData data) {
		cb.receiveData(cid, data);
	}

	@Override
	protected void notifySubscription(AClient ac) {
		super.notifySubscription(ac);
		cb.onSubscription(this.id, ac.id, ac.config);
	}

	@Override
	protected void notifyUnsubscription(AClient ac) {
		super.notifyUnsubscription(ac);
		cb.onUnsubscription(this.id, ac.id);
	}

	@Override
	public String toString() {
		return "LocalService [" + id + "]";
	}

	public void sendData(ServiceData data) {
		if (desc.type == TransmissionType.UDP_BROADCAST) {
			/* it's enough to send one broadcast packet for all clients.
			 * we mark BCAST packets by using the serviceID as destination,
			 * creating something like BCAST groups for each service */
			ServiceDataPacket sdp = new ServiceDataPacket(this.id, this.id, data, desc.type, desc.tos);
			DataTransmit.getInstance().sendBroadcast(sdp.toByteBuffers(), desc.tos);
		}
		for (AClient ac : clients.values()) {
			/* local clients need to get the bcast data individually,
			 * otherwise this applies to non-bcast data */
			if (ac instanceof LocalClient || desc.type != TransmissionType.UDP_BROADCAST) {
				ac.receiveData(this.id, data, desc.type, desc.tos);
			}
		}
	}
}
