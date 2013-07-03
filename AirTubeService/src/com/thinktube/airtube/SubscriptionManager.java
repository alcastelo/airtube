package com.thinktube.airtube;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import android.os.AsyncTask;
import android.util.Log;

import com.thinktube.net.TCPClient;
import com.thinktube.net.TCPReceiveCallback;
import com.thinktube.net.TCPServerThread;

public class SubscriptionManager {
	TCPServerThread recv;
	public final int port = 9991;
	private ServiceRegistry services;
	private Peers peers;
	private DataTransmit dt;
	private static final String TAG = null;

	public SubscriptionManager(ServiceRegistry localServices, Peers peers, DataTransmit dt) {
		this.services = localServices;
		this.peers = peers;
		this.dt = dt;
	}

	public void start() {

		recv = new TCPServerThread(port);
		recv.setReceiveCallback(new TCPReceiveCallback() {
			@Override
			public void handleLine(String line, BufferedReader in,
					final PrintWriter out, Socket socket) {
				Log.d(TAG, "from client: " + line);

				if (line.startsWith("SUBSCRIBE")) {
					String servName = line.substring(10);
					Log.d(TAG, "*SERV '" + servName + "'");
					Log.d(TAG, "*IP " + socket.getInetAddress().toString());

					Peer p = peers.get(socket.getInetAddress());
					if (p != null) {
						RemoteClient rc = new RemoteClient(p, dt);
						services.addClient(servName, rc);
					}
				}

				if (line.startsWith("UNSUBSCRIBE")) {
					String servName = line.substring(12);
					Log.d(TAG, "*SERV '" + servName + "'");
					Log.d(TAG, "*IP " + socket.getInetAddress().toString());

					Peer p = peers.get(socket.getInetAddress());
					if (p != null) {
						// localServ.removeClient(servName, p);
					}
				}
			}
		});
		recv.start();
	}

	public void stop() {
		recv.stopRunning();
	}

	public void subscribe(final String name, final InetAddress ip) {
		Log.d(TAG, "subscribe client to " + ip.toString());
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... x) {
				TCPClient client;
				try {
					client = new TCPClient(ip, port);
					client.send("SUBSCRIBE " + name);
					client.close();
					Log.d(TAG, "subscribe done");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return null;
				}
				return null;
			}
		}.execute();

	}
}
