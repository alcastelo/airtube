package example.common;

import com.thinktube.airtube.*;
import java.util.logging.Logger;

public class SimpleClient extends AirTubeComponent {
	private final static Logger LOG = Logger.getLogger(SimpleClient.class.getSimpleName());
	private final ServiceDescription serv = new ServiceDescription("simple");
	private AirTubeInterfaceI airtube;
	private AirTubeID serviceId;
	private AirTubeID clientId;

	public SimpleClient() {
	}

	@Override
	public void onConnect(AirTubeInterfaceI airtube) {
		LOG.info("connected");
		this.airtube = airtube;
		clientId = airtube.registerClient(this);
		airtube.findServices(serv, clientId);
	}

	@Override
	public void onDisconnect() {
		LOG.info("disconnected");
		serviceId = null;
		clientId = null;
		airtube = null;
	}

	@Override
	public void onServiceFound(AirTubeID id, ServiceDescription desc) {
		LOG.info("found service " + id);
		if (serviceId == null) {
			airtube.subscribeService(id, clientId, null);
		}
	}

	@Override
	public void onSubscription(AirTubeID sid, AirTubeID cid, ConfigParameters conf) {
		LOG.info("subscribed to " + sid);
		serviceId = sid;
	}

	@Override
	public void receiveData(AirTubeID sid, ServiceData data) {
		LOG.info("received '" + new String(data.data) + "' from " + sid);
	}

	@Override
	public void onUnsubscription(AirTubeID sid, AirTubeID cid) {
		LOG.info("unsubscribed from " + sid);
		serviceId = null;
	}

	@Override
	public void unregister() {
		if (serviceId != null && clientId != null) {
			airtube.unsubscribeService(serviceId, clientId);
		}
		if (clientId != null) {
			airtube.unregisterClient(clientId);
		}
		LOG.info("unregistered");
	}
}
