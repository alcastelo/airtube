package example.common;

import com.thinktube.airtube.*;
import java.util.logging.Logger;
import java.util.Timer;
import java.util.TimerTask;

public class SimpleService extends AirTubeComponent {
	private final static Logger LOG = Logger.getLogger(SimpleService.class.getSimpleName());
	private final ServiceDescription myServ = new ServiceDescription("simple", TransmissionType.UDP);
	private Timer timer;
	private final String text = "Hello AirTube World";
	private final ServiceData sd = new ServiceData(text.getBytes());
	private AirTubeInterfaceI air;
	private AirTubeID myId;

	public SimpleService() {
		timer = new Timer("Service Test Timer");
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (air == null || myId == null) {
					return;
				}
				LOG.info("send '" + text + "'");
				air.sendServiceData(myId, sd);
			}
		}, 1000L, 3 * 1000L);
	}

	@Override
	public void onConnect(AirTubeInterfaceI airtube) {
		LOG.info("AirTube connected");
		air = airtube;
		myId = air.registerService(myServ, this);
		LOG.info("registered myself as " + myId);
	}

	@Override
	public void onDisconnect() {
		LOG.info("AirTube disconnected");
		unregister();
	}

	@Override
	public void onSubscription(AirTubeID sid, AirTubeID cid, ConfigParameters config) {
		LOG.info("client " + cid + " subscribed");
	}

	@Override
	public void onUnsubscription(AirTubeID sid, AirTubeID cid) {
		LOG.info("client " + cid + " unsubscribed");
	}

	@Override
	public void unregister() {
		if (myId != null && air != null) {
			air.unregisterService(myId);
			myId = null;
		}
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}
}
