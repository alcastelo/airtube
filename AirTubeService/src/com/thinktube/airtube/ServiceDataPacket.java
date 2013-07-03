package com.thinktube.airtube;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.thinktube.net.Packet;

public class ServiceDataPacket extends Packet {
	static final byte version = 1;
	public String name;
	public String data;

	public ServiceDataPacket(byte[] buf) throws IOException {
		fromByteArray(buf);
	}

	public ServiceDataPacket(String name, String data) {
		this.name = name;
		this.data = data;
	}

	@Override
	public void readData(DataInputStream in) throws IOException {
		byte pktVersion = in.readByte();

		if (pktVersion != version)
			throw new IOException("unknown version: " + pktVersion);

		name = in.readUTF();
		data = in.readUTF();
	}

	@Override
	public void writeData(DataOutputStream out) throws IOException {
		out.writeByte(version);
		out.writeUTF(name);
		out.writeUTF(data);
	}

	@Override
	public String toString() {
		return "SD " + name + ": " + data;
	}
}
