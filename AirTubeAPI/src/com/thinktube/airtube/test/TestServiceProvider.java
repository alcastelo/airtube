package com.thinktube.airtube.test;

import com.thinktube.airtube.*;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

public class TestServiceProvider extends AirTubeComponent {
	private final static Logger LOG = Logger.getLogger(TestServiceProvider.class.getSimpleName());

	ServiceDescription myServ = new ServiceDescription("test", TransmissionType.UDP, TrafficClass.NORMAL);
	AirTubeInterfaceI air;
	AirTubeID myId;

	Timer timer;
	int sendSequenceNum = 0;
	int subscriptionCount;
	boolean periodicMessages = false;

	public TestServiceProvider() {
	}

	@Override
	public void onConnect(AirTubeInterfaceI airtube) {
		LOG.info("*** Connected to AirTube");
		this.air = airtube;

		register();
	}

	@Override
	public void onDisconnect() {
		LOG.info("*** Disconnected from AirTube");
		stop();
	}

	@Override
	public void onSubscription(AirTubeID sid, AirTubeID cid, ConfigParameters config) {
		LOG.info("* Client subscribed: " + cid + " (number of clients " + air.getNumberOfClients(myId) + ")");
		if (periodicMessages && timer == null) {
			start();
		}
		subscriptionCount++;
	}

	@Override
	public void onUnsubscription(AirTubeID sid, AirTubeID cid) {
		LOG.info("* Client unsubscribed: " + cid);
		if (--subscriptionCount == 0) {
			stop();
		}
	}

	@Override
	public void receiveData(AirTubeID clientId, ServiceData data) {
		String s = new String(data.data);
		LOG.info("Received '" + s + "' from " + clientId);

		if (!s.startsWith("Client Reply")) {
			String text = "Service Reply " + s;
			LOG.info("Sending '" + text + "' to " + clientId);
			ServiceData sd = new ServiceData(text.getBytes());
			air.sendData(clientId, myId, sd);
		}
	}

	private void register() {
		myServ.config.put("test", "Test String");
		myServ.config.put("int", 21);

		myId = air.registerService(myServ, this);

		LOG.info("* Registered as service " + myId + " with " + myServ + " and " + myServ.config);
	}

	public void unregister() {
		if (myId != null) {
			air.unregisterService(myId);
			LOG.info("* Unregistered " + myServ);
			myId = null;
		}
	}

	public void start() {
		timer = new Timer("Service Test Timer");
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (myId == null) {
					return;
				}
				String text = "Periodic Test Message " + sendSequenceNum++;
				ServiceData sd = new ServiceData(text.getBytes());
				LOG.info("Sending '" + text + "'");
				air.sendServiceData(myId, sd);
			}
		}, 1000L, 3 * 1000L);
	}

	public void stop() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	public void testSend() {
		ServiceData sd = new ServiceData("TEST".getBytes());
		LOG.info("Sending 'TEST'");
		air.sendServiceData(myId, sd);
	}

	public void periodicMessages(boolean val) {
		if (periodicMessages && !val) {
			stop();
		} else if (!periodicMessages && val && subscriptionCount > 0) {
			start();
		}
		periodicMessages = val;
	}

	public void setTransmissionType(TransmissionType tt) {
		LOG.info("set " + tt);
		ServiceDescription newDesc = new ServiceDescription("test", tt, myServ.tos);
		if (!myServ.equals(newDesc)) {
			unregister();
			myServ = newDesc;
			register();
		}
	}

	public void setTrafficClass(TrafficClass tos) {
		LOG.info("set " + tos);
		ServiceDescription newDesc = new ServiceDescription("test", myServ.type, tos);
		if (!myServ.equals(newDesc)) {
			unregister();
			myServ = newDesc;
			register();
		}
	}
}
