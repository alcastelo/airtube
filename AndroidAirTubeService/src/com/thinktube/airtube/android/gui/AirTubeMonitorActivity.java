package com.thinktube.airtube.android.gui;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

import com.thinktube.airtube.AirTubeID;
import com.thinktube.airtube.ServiceDescription;
import com.thinktube.airtube.MonitorCallbackI.Location;
import com.thinktube.airtube.android.MonitorMessenger;
import com.thinktube.airtube.android.R;
import com.thinktube.net.InetAddrUtil;

import android.util.Log;
import android.app.ActionBar;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;

public class AirTubeMonitorActivity extends FragmentActivity implements
		ControlFragment.ControlListener, TracerouteFragment.TraceListener {
	protected static final String TAG = "AirTubeGUI";
	Intent intent = new Intent("AirTube_monitor");

	ControlFragment cf = new ControlFragment();
	PeersFragment pf = new PeersFragment();
	ServicesFragment sf = new ServicesFragment();
	ClientsFragment clf = new ClientsFragment();
	SubscriptionsFragment suf = new SubscriptionsFragment();
	TracerouteFragment tf = new TracerouteFragment();
	//LogFragment lf = new LogFragment();

	ViewPager viewPager;

	private boolean monitorRegistered = false;
	private boolean airTubeStarted = false;
	private InetAddress proxyIp = null;

	protected Handler myHandler = new MsgHandler(this);
	private Messenger myMessenger = new Messenger(myHandler);
	private Messenger airtubeMessenger;

	private static class MsgHandler extends Handler {
		private final WeakReference<AirTubeMonitorActivity> ref;

		MsgHandler(AirTubeMonitorActivity act) {
			this.ref = new WeakReference<AirTubeMonitorActivity>(act);
		}

		@Override
		public void handleMessage(Message msg) {
			AirTubeMonitorActivity act = ref.get();
			if (act == null)
				return;

			Bundle b = msg.getData();
			b.setClassLoader(act.getClassLoader());
			AirTubeID id = b.getParcelable("id"); // used in many cases below, or null
			switch (msg.what) {
			case MonitorMessenger.ADD_SERVICE:
				ServiceDescription desc = b.getParcelable("desc");
				int type = b.getInt("type");
				act.log("add service " + id + " " + desc);
				act.sf.addItem(act.sf.new ServInfo(id, desc, Location.values()[type]));
				break;
			case MonitorMessenger.REMOVE_SERVICE:
				act.log("remove service " + id);
				act.sf.removeItem(act.sf.new ServInfo(id, null, Location.REMOTE));
				break;
			case MonitorMessenger.ADD_CLIENT:
				int type2 = b.getInt("type");
				act.log("add client " + id);
				act.clf.addItem(act.clf.new ClientInfo(id, Location.values()[type2]));
				break;
			case MonitorMessenger.REMOVE_CLIENT:
				act.log("remove client " + id);
				act.clf.removeItem(act.clf.new ClientInfo(id, Location.LOCAL));
				break;
			case MonitorMessenger.ADD_SUBSCRIPTION:
				AirTubeID sid = b.getParcelable("serviceId");
				AirTubeID cid = b.getParcelable("clientId");
				act.log("add subscription " + sid + " " + cid);
				act.suf.addItem(act.suf.new SubsInfo(sid, cid));
				break;
			case MonitorMessenger.REMOVE_SUBSCRIPTION:
				AirTubeID sid2 = b.getParcelable("serviceId");
				AirTubeID cid2 = b.getParcelable("clientId");
				act.log("remove subscription " + sid2 + " " + cid2);
				act.suf.removeItem(act.suf.new SubsInfo(sid2, cid2));
				break;
			case MonitorMessenger.ADD_PEER:
				long did = b.getLong("deviceId");
				act.log("add peer " + Long.toHexString(did));
				act.pf.addItem(act.pf.new PeerInfo(did));
				break;
			case MonitorMessenger.REMOVE_PEER:
				long did2 = b.getLong("deviceId");
				act.log("remove peer " + Long.toHexString(did2));
				act.pf.removeItem(act.pf.new PeerInfo(did2));
				break;
			case MonitorMessenger.UPDATE_PEER:
				long did4 = b.getLong("deviceId");
				//act.log("update peer " + Long.toHexString(did4) + ": " + b);
				PeersFragment.PeerInfo pi = act.pf.new PeerInfo(did4);
				act.pf.updateItem(pi, b);
				break;
			case MonitorMessenger.SET_IP:
				String[] ips = b.getStringArray("ips");
				String iface = b.getString("iface");
				act.log("set IP " + Arrays.toString(ips) + " on " + iface);
				act.cf.setIP(iface, ips);
				break;
			case MonitorMessenger.SET_DEVICE_ID:
				long did3 = b.getLong("deviceId");
				act.log("got device ID " + Long.toHexString(did3));
				act.cf.setDeviceID(did3);
				break;
			case MonitorMessenger.SET_STATE:
				act.cf.updateState(b.getBoolean("started"), true, b.getBoolean("onDemandEnabled"));
				break;
			case MonitorMessenger.TRACEROUTE_RESULT:
				act.tf.setResult(b.getStringArray("trace"));
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	private ServiceConnection serviceConn = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			airtubeMessenger = new Messenger(service);
			log("Connected to AirTube");
			registerMonitor();
			if (proxyIp != null) {
				sendProxyMsg();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			log("AirTube service has unexpectedly disconnected");
			airtubeMessenger = null;
			monitorRegistered = false;
			airTubeStarted = false;
			cf.updateState(false, false, false);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		viewPager = (ViewPager) findViewById(R.id.pager);

		TabsPagerAdapter tabsPager = new TabsPagerAdapter(getSupportFragmentManager(), getActionBar(), viewPager);
		tabsPager.addTabFragment("Control", cf);
		tabsPager.addTabFragment("Peers", pf);
		tabsPager.addTabFragment("Services", sf);
		tabsPager.addTabFragment("Clients", clf);
		tabsPager.addTabFragment("Subscriptions", suf);
		tabsPager.addTabFragment("Traceroute", tf);
		//tabsPager.addTabFragment("Log", lf);

	    viewPager.setAdapter(tabsPager);
        viewPager.setOnPageChangeListener(tabsPager);

        start();
	}

	public void registerMonitor() {
		if (airtubeMessenger == null)
			return;

		clear();
		Message msg = Message.obtain(null, MonitorMessenger.REGISTER_MONITOR);
		msg.replyTo = myMessenger;
		try {
			airtubeMessenger.send(msg);
			monitorRegistered = true;
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private void sendMsg(int code) {
		if (airtubeMessenger == null)
			return;

		Message msg = Message.obtain(null, code);
		try {
			airtubeMessenger.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void unregisterMonitor() {
		sendMsg(MonitorMessenger.UNREGISTER_MONITOR);
		monitorRegistered = false;
		airtubeMessenger = null;
	}

	@Override
	public void start() {
		if (airTubeStarted) {
			Log.d(TAG, "AirTube already started");
			return;
		}

		if (startService(intent) != null) {
			log("Airtube service started");
		} else {
			log("Airtube service does not exist");
			return;
		}

		if (bindService(intent, serviceConn, Context.BIND_AUTO_CREATE)) {
			log("bound");
		} else {
			log("service not bound");
		}
		airTubeStarted = true;
	}

	private void clear() {
		sf.clear();
		pf.clear();
		clf.clear();
		suf.clear();
		//lf.clear();
	}

	@Override
	public void stop() {
		unregisterMonitor();
		unbindService(serviceConn);
		stopService(intent);
		monitorRegistered = false;
		airTubeStarted = false;
		cf.updateState(false, false, false);
		log("stopped AirTube");
	}

	@Override
	public void flush() {
		sendMsg(MonitorMessenger.CLEAR_ALL);
		clear();
		log("cleared all");
	}

	@Override
	public boolean isMonitorConnected() {
		return monitorRegistered;
	}

	@Override
	public boolean isStarted() {
		return airTubeStarted;
	}

	private void log(final String msg) {
		Log.d(TAG, msg + " {" + Thread.currentThread().getId() + "}");
		//lf.log(msg);
	}

	@Override
	public void setProxy(InetAddress ip) {
		proxyIp = ip;
		sendProxyMsg();
	}

	private void sendProxyMsg() {
		if (airtubeMessenger == null)
			return;

		Message msg = Message.obtain(null, MonitorMessenger.SET_PROXY);
		if (proxyIp != null)
			msg.arg1 = InetAddrUtil.InetAddressToInt(proxyIp);
		else
			msg.arg1 = 0;

		try {
			airtubeMessenger.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void test() {
		sendMsg(MonitorMessenger.TEST);
	}

	@Override
	public void startTrace(long dst, int type, int interval) {
		if (airtubeMessenger == null)
			return;

		Message msg = Message.obtain(null, MonitorMessenger.START_TRACEROUTE);
		Bundle b = new Bundle();
		b.putLong("dst", dst);
		b.putInt("type", type);
		b.putInt("interval", interval);
		msg.setData(b);
		try {
			airtubeMessenger.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stopTrace() {
		sendMsg(MonitorMessenger.STOP_TRACEROUTE);
	}

	public void switchToTrace(int pos) {
		viewPager.setCurrentItem(5);
		tf.spinDst.setSelection(pos);
	}

	@Override
	public List<PeersFragment.PeerInfo> getPeersList() {
		return pf.list;
	}

	@Override
	public void setOnDemandEnabled(boolean on) {
		if (airtubeMessenger == null)
			return;

		Message msg = Message.obtain(null, MonitorMessenger.SET_ONDEMAND);
		msg.arg1 = on ? 1 : 0;
		try {
			airtubeMessenger.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
}
