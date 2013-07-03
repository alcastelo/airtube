package com.thinktube.airtube;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.thinktube.net.Packet;

public class ServiceAnnouncePacket extends Packet {
	static final byte version = 1;
	public volatile int seqNo = 0;
	public List<ServiceDescription> services;

	public ServiceAnnouncePacket(byte[] buf) throws IOException {
		this.services = new ArrayList<ServiceDescription>();
		fromByteArray(buf);
	}

	public ServiceAnnouncePacket(List<ServiceDescription> services) {
		this.services = services;
	}

	@Override
	public void readData(DataInputStream in) throws IOException {
		byte pktVersion = in.readByte();

		if (pktVersion != version)
			throw new IOException("unknown version: " + pktVersion);

		seqNo = in.readInt();
		int size = in.readInt();
		services.clear();
		for (int i = 0; i < size; i++) {
			String name = in.readUTF();
			services.add(new ServiceDescription(name));
		}
	}

	@Override
	public void writeData(DataOutputStream out) throws IOException {
		out.writeByte(version);
		out.writeInt(seqNo);
		out.writeInt(services.size());
		for (ServiceDescription sd : services) {
			out.writeUTF(sd.name);
		}
	}

	@Override
	public String toString() {
		String s = new String();
		for (ServiceDescription sd : services) {
			s = s + ", " + sd.name;
		}
		return "SA " + seqNo + s;
	}
}
