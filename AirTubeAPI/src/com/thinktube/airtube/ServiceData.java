package com.thinktube.airtube;

/**
 * Abstraction for the data being sent to and from services.
 */
public class ServiceData {
	public byte[] data;
	protected int len;

	public ServiceData() {
	}

	/**
	 * Copy constructor
	 */
	public ServiceData(ServiceData d) {
		if (d != null) {
			this.data = d.data;
			this.len = d.len;
		}
	}

	/**
	 * Constructor from byte array
	 * 
	 * @param data
	 *            data to be sent
	 */
	public ServiceData(byte[] data) {
		this.data = data;
		this.len = data.length;
	}

	/**
	 * Constructor from byte array with length
	 * 
	 * @param data
	 *            data to be sent
	 * @param len
	 *            length of data
	 */
	public ServiceData(byte[] data, int len) {
		this.data = data;
		this.len = len;
	}
}
