package com.thinktube.airtube;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

import android.util.Log;

import com.thinktube.net.UDPReceiveCallback;
import com.thinktube.net.UDPReceiverThread;
import com.thinktube.net.UDPSender;

public class DataTransmit {
	UDPSender udpSender;
	UDPReceiverThread udpRecv;
	private final ServiceRegistry services;
	private static final int PORT = 9992;
	protected static final String TAG = "DataTransmit";

	public DataTransmit(final ServiceRegistry services) {
		this.services = services;
	}

	public void start() {
		udpSender = new UDPSender(PORT);
		udpRecv = new UDPReceiverThread(PORT);

		udpRecv.setReceiveCallback(new UDPReceiveCallback() {
			public void handlePacket(DatagramPacket p) {
				ServiceDataPacket sdp;
				try {
					sdp = new ServiceDataPacket(p.getData());
					Log.d(TAG, "*** RECV " + sdp.toString());

					ServiceData sd = new ServiceData(sdp.data);

					AService as = services.findRemoteService(sdp.name);
					if (as == null) {
						Log.d(TAG, "service '" + sdp.name + "' not found");
						return;
					}

					for (AClient ac : as.clients) {
						ac.receiveData(as, sd);
					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		udpRecv.start();
		Log.d(TAG, "inititalized DT");
	}

	public void sendUDP(InetAddress ip, AService as, ServiceData data) {
		ServiceDataPacket p = new ServiceDataPacket(as.desc.name, data.test);
		try {
			udpSender.send(ip, p.toByteArray());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void stop() {
		udpRecv.stopRunning();
		udpSender.close();
	}
}
