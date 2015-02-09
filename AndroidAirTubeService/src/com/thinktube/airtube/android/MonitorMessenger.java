package com.thinktube.airtube.android;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.List;

import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.thinktube.airtube.AirTubeID;
import com.thinktube.airtube.MonitorCallbackI;
import com.thinktube.airtube.MonitorInterfaceI;
import com.thinktube.airtube.ServiceDescription;
import com.thinktube.net.InetAddrUtil;
import com.thinktube.net.NetworkInterfaces;
import com.thinktube.net.NetworkInterfaces.NetIf;

import android.util.Log;

public class MonitorMessenger implements MonitorCallbackI {
	protected static final String TAG = "MonitorMessenger";

	/* GUI to AirTube */
	public static final int REGISTER_MONITOR = 1;
	public static final int UNREGISTER_MONITOR = 2;
	public static final int CLEAR_ALL = 3;
	public static final int SET_PROXY = 4;
	public static final int TEST = 5;
	public static final int START_TRACEROUTE = 6;
	public static final int STOP_TRACEROUTE = 7;
	public static final int SET_ONDEMAND = 8;

	/* AirTube to GUI */
	public static final int ADD_SERVICE = 0;
	public static final int REMOVE_SERVICE = 1;
	public static final int ADD_CLIENT = 2;
	public static final int REMOVE_CLIENT = 3;
	public static final int ADD_SUBSCRIPTION = 4;
	public static final int REMOVE_SUBSCRIPTION = 5;
	public static final int ADD_PEER = 6;
	public static final int REMOVE_PEER = 7;
	public static final int UPDATE_PEER = 8;
	public static final int SET_IP = 9;
	public static final int SET_DEVICE_ID = 10;
	public static final int SET_STATE = 11;
	public static final int TRACEROUTE_RESULT = 12;

	private final Messenger myMessenger;
	private Messenger guiMessenger;
	private final MonitorInterfaceI airtube;

	private static class IncomingHandler extends Handler {
		private final WeakReference<MonitorMessenger> monRef;

		IncomingHandler(MonitorMessenger at) {
			this.monRef = new WeakReference<MonitorMessenger>(at);
		}

		@Override
		public void handleMessage(Message msg) {
			MonitorMessenger mon = monRef.get();
			if (mon == null) {
				return;
			}

			switch (msg.what) {
			case REGISTER_MONITOR:
				mon.guiMessenger = msg.replyTo; // keep reference to GUI Messenger
				try {
					mon.guiMessenger.getBinder().linkToDeath(mon.tod, 0);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				mon.airtube.registerMonitor(mon);
				break;
			case UNREGISTER_MONITOR:
				mon.guiMessenger = null;
				mon.airtube.unregisterMonitor();
				break;
			case CLEAR_ALL:
				mon.airtube.clearAll();
				break;
			case SET_PROXY:
				int iip = msg.arg1;
				InetAddress ip = null;
				if (iip != 0)
					ip = InetAddrUtil.intToInetAddress(iip);
				mon.airtube.setProxy(ip);
				break;
			case TEST:
				mon.airtube.testFunction();
				break;
			case START_TRACEROUTE:
				Bundle b = msg.getData();
				mon.airtube.tracerouteStart(b.getLong("dst"), b.getInt("type"), b.getInt("interval"));
				break;
			case STOP_TRACEROUTE:
				mon.airtube.tracerouteStop();
				break;
			case SET_ONDEMAND:
				mon.airtube.setOnDemandDV(msg.arg1 != 0);
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	private final IBinder.DeathRecipient tod = new IBinder.DeathRecipient() {
		@Override
		public void binderDied() {
			Log.d(TAG, "Binder died, unregistering monitor");
			guiMessenger = null;
			airtube.unregisterMonitor();
		}
	};

	public MonitorMessenger(MonitorInterfaceI at) {
		this.airtube = at;
		myMessenger = new Messenger(new IncomingHandler(this));
	}

	public IBinder getBinder() {
		return myMessenger.getBinder();
	}

	private void sendToGUI(int code, Bundle data) {
		if (guiMessenger == null)
			return;

		Message msg = Message.obtain(null, code);
		msg.setData(data);

		try {
			guiMessenger.send(msg);
		} catch (DeadObjectException de) {
			handleDeadObjectEx(de);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void addService(AirTubeID id, ServiceDescription desc, Location type) {
		Bundle b = new Bundle();
		b.putParcelable("id", new AirTubeIDParcel(id));
		b.putParcelable("desc", new ServiceDescriptionParcel(desc));
		b.putInt("type", type.ordinal());
		sendToGUI(ADD_SERVICE, b);
	}

	@Override
	public void removeService(AirTubeID id) {
		Bundle b = new Bundle();
		b.putParcelable("id", new AirTubeIDParcel(id));
		sendToGUI(REMOVE_SERVICE, b);
	}

	@Override
	public void addClient(AirTubeID id, Location type) {
		Bundle b = new Bundle();
		b.putParcelable("id", new AirTubeIDParcel(id));
		b.putInt("type", type.ordinal());
		sendToGUI(ADD_CLIENT, b);
	}

	@Override
	public void removeClient(final AirTubeID id) {
		Bundle b = new Bundle();
		b.putParcelable("id", new AirTubeIDParcel(id));
		sendToGUI(REMOVE_CLIENT, b);
	}

	@Override
	public void addSubscription(AirTubeID serviceId, AirTubeID clientId) {
		Bundle b = new Bundle();
		b.putParcelable("serviceId", new AirTubeIDParcel(serviceId));
		b.putParcelable("clientId", new AirTubeIDParcel(clientId));
		sendToGUI(ADD_SUBSCRIPTION, b);
	}

	@Override
	public void removeSubscription(AirTubeID serviceId, AirTubeID clientId) {
		Bundle b = new Bundle();
		b.putParcelable("serviceId", new AirTubeIDParcel(serviceId));
		b.putParcelable("clientId", new AirTubeIDParcel(clientId));
		sendToGUI(REMOVE_SUBSCRIPTION, b);
	}

	@Override
	public void addPeer(long deviceId) {
		Bundle b = new Bundle();
		b.putLong("deviceId", deviceId);
		sendToGUI(ADD_PEER, b);
	}

	@Override
	public void removePeer(long deviceId) {
		Bundle b = new Bundle();
		b.putLong("deviceId", deviceId);
		sendToGUI(REMOVE_PEER, b);
	}

	@Override
	public void updatePeer(long deviceId, long lastTime, String info) {
		Bundle b = new Bundle();
		b.putLong("deviceId", deviceId);
		b.putLong("lastTime", lastTime);
		if (info != null) b.putString("info", info);
		sendToGUI(UPDATE_PEER, b);
	}

	@Override
	public void setInterfaces(List<NetworkInterface> intf) {
		/* here we only support one interface with multiple IPs, can be extended later */
		Bundle b = new Bundle();
		NetworkInterfaces nifs = new NetworkInterfaces(intf);

		for (NetIf ni : nifs.getInterfaces()) {
			b.putString("iface", ni.getName());
			String[] ips = new String[1];
			ips[0] = ni.getAddress() + "/" + ni.getNetworkPrefixLength();
			b.putStringArray("ips", ips);
		}

		sendToGUI(SET_IP, b);
	}

	@Override
	public void setDeviceID(long deviceId) {
		Bundle b = new Bundle();
		b.putLong("deviceId", deviceId);
		sendToGUI(SET_DEVICE_ID, b);
	}

	@Override
	public void setState(boolean started, boolean onDemandEnabled) {
		Bundle b = new Bundle();
		b.putBoolean("started", started);
		b.putBoolean("onDemandEnabled", onDemandEnabled);
		sendToGUI(SET_STATE, b);
	}

	private void handleDeadObjectEx(DeadObjectException de) {
		Log.e(TAG, "DeadObjectException, unregistering monitor");
		this.guiMessenger = null;
		airtube.unregisterMonitor();
	}

	@Override
	public void traceRouteResult(String[] trace) {
		Bundle b = new Bundle();
		b.putStringArray("trace", trace);
		sendToGUI(TRACEROUTE_RESULT, b);
	}
}
