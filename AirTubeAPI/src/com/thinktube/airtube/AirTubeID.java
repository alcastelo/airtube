package com.thinktube.airtube;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * AirTubeIDs are used to uniquely and globally identify AirTube components
 * (clients or services) which are registered with the AirTube system.
 * <p>
 * The AirTubeID consists of a 64bit device ID and a locally managed ("port")
 * number and is generated and assigned by AirTube to components when they
 * register. API users are not supposed to change them. Components identify
 * themselves and other services throughout the API by using AirTubeIDs.
 * <p>
 * Note: The use of the AirTubeID instead of direct object references is another
 * design decision to facilitate the efficient use of AirTube on Android.
 * <p>
 * TODO: The details should not be visible to the user!
 */
public class AirTubeID {
	public long deviceId;
	public short port;

	public AirTubeID(short port, long deviceId) {
		this.port = port;
		this.deviceId = deviceId;
	}

	public AirTubeID(AirTubeID id) {
		if (id != null) {
			this.port = id.port;
			this.deviceId = id.deviceId;
		}
	}

	/**
	 * Create from String as provided by {@link #getString}
	 * 
	 * @param s
	 *            String as provided by {@link #getString}
	 */
	public AirTubeID(String s) {
		if (s != null) {
			int sep = s.indexOf('/');
			port = Short.parseShort(s.substring(0, sep));
			deviceId = new BigInteger(s.substring(sep + 1, s.length()), 16)
					.longValue();
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (deviceId ^ (deviceId >>> 32));
		result = prime * result + port;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		/* Note: remove class check for AirTubeIDParcel */
		// if (getClass() != obj.getClass())
		// return false;
		AirTubeID other = (AirTubeID) obj;
		if (deviceId != other.deviceId)
			return false;
		if (port != other.port)
			return false;
		return true;
	}

	/**
	 * This is a shorter version of {@link #toString}, which returns the same
	 * format as the constructor accepts and is possible to use in a network
	 * protocol
	 */
	public String getString() {
		return port + "/" + Long.toHexString(deviceId);
	}

	public String toString() {
		return "AirTubeID [" + getString() + "]";
	}

	/**
	 * Write to a DataOutputStream (TODO: use internally only)
	 * 
	 * @param out
	 * @throws IOException
	 */
	public void writeData(DataOutputStream out) throws IOException {
		out.writeShort(port);
		out.writeLong(deviceId);
	}

	/**
	 * Create from DataInputStream (TODO: use internally only)
	 * 
	 * @param in
	 * @throws IOException
	 */
	public AirTubeID(DataInputStream in) throws IOException {
		port = in.readShort();
		deviceId = in.readLong();
	}

	/**
	 * Create from ByteBuffer
	 *
	 * @param in
	 * @throws IOException
	 */
	public AirTubeID(ByteBuffer in) {
		port = in.getShort();
		deviceId = in.getLong();
	}

	/**
	 * Write to a ByteBuffer
	 *
	 * @param out
	 */
	public void toByteBuffer(ByteBuffer out) {
		out.putShort(port);
		out.putLong(deviceId);
	}
}
