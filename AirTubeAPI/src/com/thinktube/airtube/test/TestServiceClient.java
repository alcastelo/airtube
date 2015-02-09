package com.thinktube.airtube.test;

import com.thinktube.airtube.*;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class TestServiceClient extends AirTubeComponent {
	private final static Logger LOG = Logger.getLogger(TestServiceClient.class.getSimpleName());
	 // don't specify TransmissionType or TrafficClass since we don't care:
	private ServiceDescription serv = new ServiceDescription("test");
	private List<AirTubeID> serviceIds = new CopyOnWriteArrayList<AirTubeID>();
	private AirTubeInterfaceI airtube;
	private AirTubeID clientId;

	public TestServiceClient() {
	}

	@Override
	public void onConnect(AirTubeInterfaceI airtube) {
		LOG.info("*** Connected to AirTube");
		this.airtube = airtube;
		clientId = airtube.registerClient(this);
		LOG.info("* Registered as client " + clientId + " looking for services with "  + serv);
		airtube.findServices(serv, clientId);
	}

	@Override
	public void onDisconnect() {
		LOG.info("*** Disconnected from AirTube");
		serviceIds.clear();
		clientId = null;
	}

	@Override
	public void onServiceFound(AirTubeID id, ServiceDescription desc) {
		LOG.info("* Found service " + id + " with " + desc + " and " + desc.config);
		airtube.subscribeService(id, clientId, new ConfigParameters("{test = 'bla'}"));
	}

	@Override
	public void onSubscription(AirTubeID sid, AirTubeID cid, ConfigParameters conf) {
		LOG.info("* Subscribed to " + sid);
		serviceIds.add(sid);
	}

	@Override
	public void receiveData(AirTubeID sid, ServiceData data) {
		String s = new String(data.data);
		LOG.info("Received '" + s + "' from " + sid);

		if (!s.startsWith("TEST") && !s.startsWith("Service Reply")) {
			int i = Integer.parseInt(s.substring(s.lastIndexOf(' ')+1));
			String text = "Client Reply ACK " + i;
			ServiceData sd = new ServiceData(text.getBytes());
			LOG.info("Sending '" + text + "' to " + sid);
			airtube.sendData(sid, clientId, sd);
		}
	}

	@Override
	public void onUnsubscription(AirTubeID sid, AirTubeID cid) {
		LOG.info("* Unsubscribed from " + sid);
		serviceIds.remove(sid);
	}

	@Override
	public void unregister() {
		for (AirTubeID sid : serviceIds) {
			airtube.unsubscribeService(sid, clientId);
			LOG.info("* Unsubscribing " + sid);
		}
		airtube.unregisterClient(clientId);
		LOG.info("* Unregistered");
	}

	public void testSend() {
		ServiceData sd = new ServiceData("TEST".getBytes());
		for (AirTubeID sid : serviceIds) {
			LOG.info("Sending 'TEST' to " + sid);
			airtube.sendData(sid, clientId, sd);
		}
	}
}
