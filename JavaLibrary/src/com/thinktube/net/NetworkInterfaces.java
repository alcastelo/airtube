package com.thinktube.net;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/*
 * Note that a bug in Java JDK gives us wrong broadcast addresses
 * for interfaces with multiple IPs (on Linux it usually is just the
 * BCAST address of the "first" IP of the device).
 *
 * That's why we implement the abstraction of "NetworkInterfaces" and
 * "NetIf".
 *
 * NetworkInterfaces may be a subset of all available interfaces.
 *
 * NetIf contains only valid addresses.
 *
 * Later we may also add dynamic interfaces up/down monitoring?
 */
public class NetworkInterfaces {
	public interface NetIf {
		public InetAddress getAddress();
		public boolean hasBroadcast();
		public InetAddress getBroadcast();
		public short getNetworkPrefixLength();
		public String getName();
		public NetworkInterface getNetworkInterface();
		public boolean isMobile();
		public boolean isWireless();
	}

	public static class NetIfDevice implements NetIf {
		private final NetworkInterface ni;
		private final InterfaceAddress addr;
		private final boolean isMobile;
		private final boolean isWireless;

		public NetIfDevice(NetworkInterface ni, InterfaceAddress addr) {
			this.ni = ni;
			this.addr = addr;
			this.isMobile = (ni != null && ni.getName().startsWith("rmnet"));
			this.isWireless = (ni != null && ni.getName().startsWith("wlan"));
		}

		public InetAddress getAddress() {
			return addr.getAddress();
		}

		public boolean hasBroadcast() {
			return (addr.getBroadcast() != null);
		}

		public InetAddress getBroadcast() {
			return addr.getBroadcast();
		}

		public short getNetworkPrefixLength() {
			return addr.getNetworkPrefixLength();
		}

		public String getName() {
			return ni.getName();
		}

		public NetworkInterface getNetworkInterface() {
			return ni;
		}

		public String toString() {
			return ni.getName() + addr.getAddress() + "/" + addr.getNetworkPrefixLength();
		}

		public boolean isMobile() {
			return isMobile;
		}

		public boolean isWireless() {
			return isWireless;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((addr == null) ? 0 : addr.hashCode());
			result = prime * result + ((ni == null) ? 0 : ni.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			NetIfDevice other = (NetIfDevice) obj;
			if (addr == null) {
				if (other.addr != null)
					return false;
			} else if (!addr.equals(other.addr))
				return false;
			if (ni == null) {
				if (other.ni != null)
					return false;
			} else if (!ni.equals(other.ni))
				return false;
			return true;
		}
	}

	private final Set<NetIf> nifs = new CopyOnWriteArraySet<NetIf>();

	public NetworkInterfaces() {
	}

	public NetworkInterfaces(List<NetworkInterface> intf) {
		for (NetworkInterface ni : intf) {
			addInterface(ni);
		}
	}

	public void addAllInterfaces() {
		try {
			for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
				if (!ni.isLoopback() && ni.isUp() && !ni.getName().startsWith("p2p")) {
					addInterface(ni);
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	public boolean addInterface(NetworkInterface ni) {
		for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
			//System.out.println("**" + ia.getAddress() + " " + ia.getBroadcast() + " " + ia.getNetworkPrefixLength());
			if (ia.getAddress() != null && (isMobile(ni) || (ia.getBroadcast() != null &&
					InetAddrUtil.isInRage(ia.getAddress(), ia.getBroadcast(), ia.getNetworkPrefixLength())))) {
				NetIf nif = new NetIfDevice(ni, ia);
				addInterface(nif);
				return true;
			}
		}
		return false;
	}

	public boolean addInterface(NetIf nif) {
		if (nif == null) {
			throw new RuntimeException("network interface can not be null!");
		}
		return nifs.add(nif);
	}

	public NetIf findInterface(NetIf search) {
		for (NetIf ni : nifs) {
			if (ni.equals(search)) {
				return ni;
			}
		}
		return null;
	}

	public boolean addInterface(String string) {
		try {
			NetworkInterface ni = NetworkInterface.getByName(string);
			if (ni == null)
				return false;
			else
				return addInterface(ni);
		} catch (SocketException e) {
			return false;
		}
	}

	public Set<NetIf> getInterfaces() {
		return nifs;
	}

	public List<NetworkInterface> getNetworkInterfaces() {
		List<NetworkInterface> ret = new ArrayList<NetworkInterface>(nifs.size());
		for (NetIf ni : nifs) {
			ret.add(ni.getNetworkInterface());
		}
		return ret;
	}

	public static boolean isWireless(NetworkInterface ni) {
		if (ni != null && ni.getName().startsWith("wlan")) {
			return true;
		} else {
			return false;
		}
	}
	public static boolean isMobile(NetworkInterface ni) {
		if (ni != null && ni.getName().startsWith("rmnet")) {
			return true;
		} else {
			return false;
		}
	}

	public String getNames() {
		StringBuilder ifNames = new StringBuilder();
		for (NetIf ni : nifs) {
			ifNames.append(" ").append(ni.getName());
		}
		return ifNames.toString();
	}

	public void removeInterface(NetIf rm) {
		nifs.remove(rm);
	}
}
