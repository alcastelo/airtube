package com.thinktube.airtube.android;

import java.util.logging.Logger;

import com.thinktube.airtube.*;
import com.thinktube.android.codec.*;
import com.thinktube.android.video.CameraPreview;
import com.thinktube.android.video.CameraPreviewHandler;
import com.thinktube.codec.CodecOutCallbackI;
import com.thinktube.codec.ProtocolSenderI;
import com.thinktube.codec.VideoProtocol;

public class VideoService extends AirTubeComponent {
	private final static Logger LOG = Logger.getLogger(VideoService.class.getSimpleName());
	private static final int DEFAULT_VIDEO_WIDTH = 480;
	private static final int DEFAULT_VIDEO_HEIGHT = 640;
	private static final int DEFAULT_VIDEO_FRAME_RATE = 15;
	private static final int DEFAULT_VIDEO_BPS = 1000000;
	private static final int DEFAULT_VIDEO_IFRAME_INT = 5;

	private ServiceDescription myServ = new ServiceDescription("video", TransmissionType.UDP, TrafficClass.VIDEO);
	private AirTubeID myId;
	AirTubeInterfaceI airtube = null;

	private CameraPreviewHandler camPreviewHandler;
	private HwVideoEncoder videoEnc;
	private int subsCount = 0;

	public VideoService(CameraPreview camPreview) {
		this(camPreview, DEFAULT_VIDEO_WIDTH, DEFAULT_VIDEO_HEIGHT, DEFAULT_VIDEO_FRAME_RATE, DEFAULT_VIDEO_BPS, DEFAULT_VIDEO_IFRAME_INT);
	}

	public VideoService(CameraPreviewHandler camPreviewH) {
		this.camPreviewHandler = camPreviewH;
		init(DEFAULT_VIDEO_WIDTH, DEFAULT_VIDEO_HEIGHT, DEFAULT_VIDEO_FRAME_RATE, DEFAULT_VIDEO_BPS, DEFAULT_VIDEO_IFRAME_INT);
	}

	public VideoService(CameraPreview camPreview, int width, int height, int frameRate, int bps, int iFrameInt) {
		camPreviewHandler = new CameraPreviewHandler(width, height);
		camPreview.setPreviewCallback(camPreviewHandler);
		init(width, height, frameRate, bps, iFrameInt);
	}

	public VideoService(CameraPreviewHandler camPreviewH, int width, int height, int frameRate, int bps, int iFrameInt) {
		this.camPreviewHandler = camPreviewH;
		init(width, height, frameRate, bps, iFrameInt);
	}

	private void init(int width, int height, int frameRate, int bps, int iFrameInt) {
		myServ.config = new ConfigParameters();
		myServ.config.put("width", width);
		myServ.config.put("height", height);
		myServ.config.put("frame-rate", frameRate);
		myServ.config.put("bps", bps);
		myServ.config.put("i-frame-interval", iFrameInt);

		CodecOutCallbackI videoCb = new CodecOutAndroidAdapter(new VideoProtocol.OutCallback(new ProtocolSenderI() {
			@Override
			public void sendPacket(byte[] bytes, int len) {
				if (airtube == null)
					return;
				ServiceData sd = new ServiceData(bytes, len);
				airtube.sendServiceData(myId, sd);
			}
		}));

		// useful for debugging:
		//CodecOutMultiAndroid videoCb = new CodecOutMultiAndroid();
		//videoCb.addCb(new CodecOutMuxerWriter());
		//videoCb.addCb(new CodecOutRawFileWriter(new File(Environment.getExternalStorageDirectory(), "test.h264")));

		videoEnc = new HwVideoEncoder(videoCb, width, height, frameRate, bps, iFrameInt);
	}

	@Override
	public void onConnect(AirTubeInterfaceI conn) {
		airtube = conn;
		prepare();
	}

	@Override
	public void onDisconnect() {
		airtube = null;
		stop();
	}

	@Override
	public void onSubscription(AirTubeID sid, AirTubeID cid, ConfigParameters config) {
		LOG.info("onSubscription " + cid);
		if (subsCount++ == 0) {
			start();
		}
	}

	@Override
	public void onUnsubscription(AirTubeID sid, AirTubeID cid) {
		LOG.info("onUnSubscription " + cid);
		if (--subsCount == 0 && videoEnc != null) {
			stop();
		}
	}

	private void prepare() {
		try {
			videoEnc.init();
			videoEnc.start();

			byte[] cb = videoEnc.getConfigBytes();
			if (cb == null) {
				LOG.severe("ERR: could not initialize codec, probably your configuration (WIDTHxHEIGHT) is wrong!");
				return;
			}

			myServ.config.put("config-bytes", cb);
			myId = airtube.registerService(myServ, this);
			LOG.info("registered myself as " + myId + " config " + myServ.config.getString());
		} catch (Exception e) {
			LOG.severe("HW codec failed to initialize, please restart App!");
			e.printStackTrace();
		}
	}

	@Override
	public void start() {
		camPreviewHandler.addListener(videoEnc);
		// already started: videoEnc.start();
	}

	@Override
	public void stop() {
		camPreviewHandler.removeListener(videoEnc);
		//videoEnc.stop();
	}

	@Override
	public void unregister() {
		if (airtube != null) {
			airtube.unregisterService(myId);
		}
		myId = null;
		//LOG.info("unregistered myself");
	}

	public AirTubeID getServiceId() {
		return myId;
	}
}
