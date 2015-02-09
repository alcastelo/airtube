package com.thinktube.util;

public class NumberConversion {

	/* default to big endian as used by IP addresses */

	public static int intFrom(byte[] b, int pos) {
		return ((b[pos+3] & 0xFF) << 24 |
				(b[pos+2] & 0xFF) << 16 |
				(b[pos+1] & 0xFF) << 8 |
				 b[pos] & 0xFF);
	}

	public static void intTo(int value, byte[] b, int pos) {
		b[pos+3] = (byte)((value >>> 24) & 0xFF);
		b[pos+2] = (byte)((value >>> 16) & 0xFF);
		b[pos+1] = (byte)((value >>> 8) & 0xFF);
		b[pos] = (byte)(value & 0xFF);
	}

	public static int intFromLE(byte[] b, int pos) {
		return ((b[pos] & 0xFF) << 24 |
				(b[pos+1] & 0xFF) << 16 |
				(b[pos+2] & 0xFF) << 8 |
				 b[pos+3] & 0xFF);
	}

	public static void intToLE(int value, byte[] b, int pos) {
		b[pos] = (byte)((value >>> 24) & 0xFF);
		b[pos+1] = (byte)((value >>> 16) & 0xFF);
		b[pos+2] = (byte)((value >>> 8) & 0xFF);
		b[pos+3] = (byte)(value & 0xFF);
	}
}
