package com.thinktube.util;

public class ArrayUtil {
	/**
	 * Replaces Arrays.fill, which happens to be too slow.
	 * this is just a hack against the JVM
	 * http://www.searchenginecaffe.com/2007/03/how-to-quickly-reset-value-of-java.html
	 * @param array
	 */
	public static void bytefillZero(byte[] array) {
		final int len = array.length;
		if (len > 0)
			array[0] = 0;
		for (int i = 1; i < len; i += i) {
			System.arraycopy(array, 0, array, i, ((len - i) < i) ? (len - i) : i);
		}
	}
}
