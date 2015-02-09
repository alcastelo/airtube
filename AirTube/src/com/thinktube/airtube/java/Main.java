package com.thinktube.airtube.java;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import com.thinktube.airtube.AirTube;
import com.thinktube.airtube.AirTubeComponent;
import com.thinktube.net.NetworkInterfaces;

public class Main {
	public static void main(String[] args) {
		NetworkInterfaces nifs = new NetworkInterfaces();
		ArrayList<String> acNames = new ArrayList<String>();
		InetAddress proxy = null;
		long id = 0;

		for (int i=0; i < args.length; i++) {
			if (args[i].equals("-i")) {
				// network interface
				if (!nifs.addInterface(args[++i])){
					System.err.println("Ignoring interface: " + args[i]);
				}
			} else if (args[i].equals("-d")) {
				// device ID
				String num = args[++i];
				if (num.startsWith("0x"))
					id = Long.parseLong(num.substring(2), 16);
				else
					id = Long.parseLong(num);
			} else if (args[i].equals("-c")) {
				// component class name to add
				acNames.add(args[++i]);
			} else if (args[i].equals("-p")) {
				// proxy IP
				try {
					proxy = InetAddress.getByName(args[++i]);
				} catch (UnknownHostException e) {
					System.err.println("ProxyIP could not be resoved, ignoring");
				}
			}
		}

		System.out.println("*** Pure Java version starting");

		AirTube at = new AirTube(id);

		if (proxy != null) {
			at.setProxy(proxy);
		}

		at.start(nifs);

		for (String acName : acNames) {
			try {
				at.addComponent((AirTubeComponent) Class.forName(acName).newInstance());
				System.out.println("Added component: " + acName);
			} catch (InstantiationException e) {
				System.err.println("Can't instantiate: " + acName);
			} catch (IllegalAccessException e) {
				System.err.println("Can't access: " + acName);
			} catch (ClassNotFoundException e) {
				System.err.println("Class not found: " + acName);
			}
		}

		System.out.println("*** Press any key to stop!");

		try {
			System.in.read();
		} catch (IOException e) { /* ignore */ }

		System.out.println("*** Stopping");

		at.stop();
		Thread.yield();

		System.out.println("*** Done");
	}
}
