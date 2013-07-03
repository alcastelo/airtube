package com.thinktube.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class Packet {

	public Packet() {
	}

	/*
	 * this is NOT possible because it would use subclass overridden functions
	 * and this can lead to unexpected results: public Packet(byte[] buf){ }
	 */

	public Packet fromByteArray(byte[] buf) throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(buf);
		DataInputStream dis = new DataInputStream(bais);

		readData(dis);

		return this;
	}

	public byte[] toByteArray() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);

		writeData(dos);

		return baos.toByteArray();
	}

	public abstract void readData(DataInputStream in) throws IOException;

	public abstract void writeData(DataOutputStream out) throws IOException;
}
