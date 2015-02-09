package com.thinktube.net;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.thinktube.util.NumberConversion;

public class InetAddrUtil {

	public static InetAddress intToInetAddress(int hostAddress) {
		InetAddress inetAddress;
		byte[] addressBytes = new byte[4];

		NumberConversion.intTo(hostAddress, addressBytes, 0);

		try {
			inetAddress = InetAddress.getByAddress(addressBytes);
		} catch (UnknownHostException e) {
			return null;
		}
		return inetAddress;
	}

	public static int InetAddressToInt(InetAddress ip) {
		if (ip == null)
			return 0;

		byte[] addressBytes = ip.getAddress();
		return NumberConversion.intFrom(addressBytes, 0);
	}

	public static boolean isInRage(InetAddress check, InetAddress bcast, int netmask) {
		//System.out.println("*** check " + check + " in " + bcast + "/" + netmask);
		int byts = netmask/8;
		// netmask%8 are the remaining bits. The mask is for the upper bits of the remaining byte
		byte mask = (byte)~(0xff >>> (netmask%8));
		byte[] b1 = check.getAddress();
		byte[] b2 = bcast.getAddress();

		int i;
		for (i=0; i<byts; i++) {
			if (b1[i] != b2[i]) {
				return false;
			}
		}

		// remaining bits
		if (mask != 0 && (b1[i] & mask) != (b2[i] & mask)) {
			return false;
		}
		return true;
	}

	public static void main(String[] args) throws UnknownHostException {
		isInRage(InetAddress.getByName("192.168.5.14"), InetAddress.getByName("192.168.5.0"), 26);
		isInRage(InetAddress.getByName("192.168.5.64"), InetAddress.getByName("192.168.5.0"), 26);
		isInRage(InetAddress.getByName("192.168.5.164"), InetAddress.getByName("192.168.5.255"), 24);
	}
}
