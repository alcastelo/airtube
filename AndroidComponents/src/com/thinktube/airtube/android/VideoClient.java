package com.thinktube.airtube.android;

import java.util.logging.Logger;
import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.os.Build;
import android.view.Surface;
import com.thinktube.airtube.*;
import com.thinktube.android.codec.HwVideoDecoder;
import com.thinktube.codec.Codec;
import com.thinktube.codec.ProtocolReceiverI;
import com.thinktube.codec.VideoProtocol;

public class VideoClient extends AirTubeComponent {
	private final static Logger LOG = Logger.getLogger(VideoClient.class.getSimpleName());
		
	ServiceDescription myServ = new ServiceDescription("video");
	AirTubeInterfaceI airtube;
	AirTubeID serviceId;
	AirTubeID myId;

	private HwVideoDecoder hwDec;
	private VideoProtocol.RecvCallback cb;
	private byte[] configBytes;
	private int width, height;
	private Surface renderSurface;

	boolean subscribed;
	boolean codecInitialized;
	boolean passive; // this indicates the client should not subscribe by itself as it will be managed from outside
	boolean autostart;

	public interface VideoClientCB {
		void onSubscribe(ConfigParameters cp);
	}

	private VideoClientCB clientCb;

	public VideoClient(boolean passive, boolean autostart) {
		this.passive = passive;
		this.autostart = autostart;
		cb = new VideoProtocol.RecvCallback(new ProtocolReceiverI() {
			@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
			@Override
			public void receiveFrame(byte[] data, int offset, int len, int flags) {
				int mcFlags = 0;
				if ((flags & Codec.FLAG_CODEC_CONFIG) != 0) {
					mcFlags = MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
				}
				if ((flags & Codec.FLAG_SYNC_FRAME) != 0) {
					mcFlags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;
				}
				hwDec.input(data, offset, len, mcFlags);
			}

			@Override
			public boolean isReady() {
				return hwDec.isReady();
			}
		});
	}

	public VideoClient() {
		this(false, true);
	}

	public VideoClient(boolean passive) {
		this(passive, true);
	}

	@Override
	public void onConnect(AirTubeInterfaceI airtube) {
		this.airtube = airtube;
		register();
	}

	@Override
	public void onDisconnect() {
		myId = null;
		stop();
	}

	@Override
	public void onServiceFound(AirTubeID id, ServiceDescription desc) {
		if (subscribed) {
			LOG.info("* service found but already subscribed");
			return;
		}

		if (id.deviceId == myId.deviceId) {
			return;
		}

		setService(id, desc);
		/* after this, and the creation of the surface the codec is initialized and running */
	}

	@Override
	public void onSubscription(AirTubeID serv, AirTubeID id, ConfigParameters conf) {
		LOG.info("onSubscription " + serv);
		subscribed = true;
		/* nothing to do since everything is initialized in onServiceFound */
		if (clientCb != null) {
			clientCb.onSubscribe(conf);
		}
	}

	@Override
	public void onUnsubscription(AirTubeID serv, AirTubeID id) {
		LOG.info("onUnSubscription " + serv);
		subscribed = false;
		stop();
	}

	@Override
	public void receiveData(AirTubeID sid, ServiceData data) {
		cb.handlePacket(data.data);
	}

	@Override
	public void start() {
		tryInit(); // this will subscribe
	}

	@Override
	public void stop() {
		if (subscribed && myId != null) {
			airtube.unsubscribeService(serviceId, myId);
			/* this will trigger below in onUnsubscribe() */
		} else {
			if (hwDec != null)
				hwDec.stop();
			cb.reset();
			codecInitialized = false;
			subscribed = false;
		}
	}

	private void register() {
		myId = airtube.registerClient(this);
		if (!passive) {
			airtube.findServices(myServ, myId);
		}
	}

	@Override
	public void unregister() {
		if (!passive) {
			airtube.unregisterServiceInterest(myServ, myId);
		}
		airtube.unsubscribeService(serviceId, myId);
		serviceId = null;
		subscribed = false;
		airtube.unregisterClient(myId);
		myId = null;
	}

	public void setSurface(Surface surface) {
		this.renderSurface = surface;
		tryInit();
	}

	public void setService(AirTubeID sid, ServiceDescription desc) {
		serviceId = sid;

		if (desc.config == null) {
			LOG.info("config null");
			return;
		}

		try {
			configBytes = desc.config.getBytes("config-bytes");
			width = desc.config.getInt("width");
			height = desc.config.getInt("height");
			if (configBytes != null) {
				LOG.info("service found: " + sid + " config " + desc.config);
				if (autostart) {
					tryInit(); // this will subscribe
				} else {
					LOG.info("service found but not starting");
				}
			}
		} catch (Exception e) {
			LOG.info("error in configBytes");
			e.printStackTrace();
		}
	}

	public AirTubeID getService() {
		return serviceId;
	}

	public void subscribeTo(AirTubeID sid) {
		ServiceDescription sd = airtube.getDescription(sid);
		setService(sid, sd);
	}

	public void unsubscribe() {
		if (subscribed && myId != null) {
			airtube.unsubscribeService(serviceId, myId);
		}
	}

	private void tryInit() {
		// either the service can be found before the surface is created or the
		// surface may be created after the service is found. We need both to initialize the codec
		// and handle that here:
		if (configBytes != null && renderSurface != null) {
			if (!codecInitialized) {
				try {
					hwDec = new HwVideoDecoder(width, height);
					hwDec.init(configBytes, renderSurface);
					hwDec.start();
					codecInitialized = true;
				} catch (Exception e) {
					LOG.info("* codec initialization failed!");
					return;
				}
			}
			// we can only subscribe after the codec has been initialized
			subscribe();
		}
	}

	private void subscribe() {
		if (subscribed) {
			LOG.info("* already subscribed");
			return;
		}
		if (serviceId == null) {
			LOG.info("* no service found yet");
			return;
		}
		LOG.info("subscribing to " + serviceId);
		airtube.subscribeService(serviceId, myId, null);
	}

	public void setCallback(VideoClientCB cb) {
		this.clientCb = cb;
	}
}
