package com.thinktube.airtube;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import com.thinktube.net.UDPPeriodicSenderThread;
import com.thinktube.net.UDPReceiveCallback;
import com.thinktube.net.UDPReceiverThread;
import com.thinktube.net.UDPSendCallback;

import android.util.Log;

public class ServiceAnnounce {
	private static final String TAG = "ServiceAnnounce";
	protected static final long PEER_TIMER_MS = 10000; // 10 sec
	public static final int PORT = 9999;
	private static final String MCAST_GROUP = "224.2.76.24";

	UDPPeriodicSenderThread st;

	UDPReceiverThread rt;
	UDPReceiveCallback recvCb;

	public InetAddress group;
	MulticastSocket socket;
	public InetAddress localIp;

	private ServiceRegistry services;
	public Peers peers;

	public ServiceAnnounce(ServiceRegistry services, Peers peers) {
		this.services = services;
		this.peers = peers;
	}

	public void connect() {
		// Do we still need to get wifi multicast lock?
		try {
			socket = new MulticastSocket(PORT);
			group = InetAddress.getByName(MCAST_GROUP);
			socket.joinGroup(group);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		localIp = getLocalIPv4("wlan0");
		Log.d(TAG, "Local IP: " + localIp.toString());
	}

	public void start() {
		connect();

		/* sending */
		st = new UDPPeriodicSenderThread(socket, group, PORT, PEER_TIMER_MS);
		st.setCallback(new UDPSendCallback() {
			int seq = 0;

			public byte[] getPacketData() throws IOException {
				ServiceAnnouncePacket sp = new ServiceAnnouncePacket(services
						.getLocalServiceDescriptions());
				sp.seqNo = seq++;
				Log.d(TAG, "Sending " + sp.toString());
				return sp.toByteArray();
			}
		});
		st.start();

		/* receiving */
		rt = new UDPReceiverThread(socket);
		rt.setReceiveCallback(new UDPReceiveCallback() {
			public void handlePacket(DatagramPacket p) {
				try {
					if (p.getAddress().equals(localIp)) {
						return; // ignore own pkt
					}

					ServiceAnnouncePacket sp = new ServiceAnnouncePacket(p.getData());
					android.util.Log.d(TAG, "received " + sp.toString());

					Peer peer = peers.addOrUpdate(p.getAddress(), sp.seqNo);
					updateServices(peer, sp);

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		});
		rt.start();
	}

	public void stop() {
		rt.stopRunning();
		st.stopRunning();
		try {
			socket.leaveGroup(group);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void updateServices(Peer peer, ServiceAnnouncePacket sp) {
		for (ServiceDescription sd : sp.services) {
			AService rs = services.findRemoteService(sd.name);
			if (rs == null) {
				rs = new AService(sd);
				services.addRemote(rs);
			}
			rs.providers.add(peer);

			Log.d(TAG,
					"update service " + sd.name + " " + rs.providers.toString());
		}
	}

	private InetAddress getLocalIPv4(String ifName) {
		try {
			NetworkInterface intf = NetworkInterface.getByName(ifName);
			for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr
					.hasMoreElements();) {
				InetAddress ia = enumIpAddr.nextElement();
				if (ia.getClass() == Inet4Address.class) {
					return ia;
				}
			}
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/*
		 * try { System.out.println("Full list of Network Interfaces:"); for
		 * (Enumeration<NetworkInterface> en =
		 * NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
		 * NetworkInterface intf = en.nextElement(); System.out.println("    " +
		 * intf.getName() + " " + intf.getDisplayName()); for
		 * (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
		 * enumIpAddr.hasMoreElements(); ) { System.out.println("        " +
		 * enumIpAddr.nextElement().toString()); } } } catch (SocketException e)
		 * { System.out.println(" (error retrieving network interface list)"); }
		 */

		/*
		 * Other option if we had a context:
		 * 
		 * android.net.wifi.WifiManager wim= (android.net.wifi.WifiManager)
		 * getSystemService(WIFI_SERVICE); WifiInfo wi =
		 * wim.getConnectionInfo(); return toInetAddress(wi.getIpAddress());
		 * 
		 * static public byte[] toIPByteArray(int addr){ return new
		 * byte[]{(byte)
		 * addr,(byte)(addr>>>8),(byte)(addr>>>16),(byte)(addr>>>24)}; }
		 * 
		 * static public InetAddress toInetAddress(int addr){ try { return
		 * InetAddress.getByAddress(toIPByteArray(addr)); } catch
		 * (UnknownHostException e) { //should never happen return null; } }
		 */

		return null;
	}
}
