package com.thinktube.util;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class ByteBufferUtil {
	public static void putString(ByteBuffer bb, String s) {
		byte[] b = s.getBytes(Charset.forName("UTF-8"));
		bb.putShort((short)b.length);
		bb.put(b);
	}


	public static String getString(ByteBuffer bb) {
		short len = bb.getShort();
		byte[] b = new byte[len];
		bb.get(b);
		return new String(b, Charset.forName("UTF-8"));
	}

	public static String getStringWOLength(ByteBuffer bb) {
		byte[] b = new byte[bb.limit()];
		bb.get(b);
		return new String(b, Charset.forName("UTF-8"));
	}
}
