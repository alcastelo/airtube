package com.thinktube.airtube;

import java.nio.ByteBuffer;

import com.thinktube.net.Packet;

public class ServiceDataPacket implements Packet {
	static final byte version = 3;
	static final byte TRANS_MASK = 0x3;
	static final byte TOS_MASK = 0xC;
	static final byte TOS_SHIFT = 2;

	public AirTubeID srcId;
	public AirTubeID dstId;
	public ServiceData data;
	public TrafficClass tos;
	public TransmissionType trans;

	public ServiceDataPacket(ByteBuffer buf) {
		fromByteBuffer(buf);
	}

	public ServiceDataPacket(AirTubeID src, AirTubeID dst, ServiceData data, TransmissionType trans, TrafficClass tos) {
		this.srcId = src;
		this.dstId = dst;
		this.data = data;
		this.trans = trans;
		this.tos = tos;
	}

	@Override
	public void fromByteBuffer(ByteBuffer in) {
		byte pktVersion = in.get();

		if (pktVersion != version)
			throw new RuntimeException("unknown version: " + pktVersion);

		readFlags(in.get());
		srcId = new AirTubeID(in);
		dstId = new AirTubeID(in);
		int length = in.getInt();
		byte[] b = new byte[length];
		in.get(b);
		data = new ServiceData(b);
	}

	private void readFlags(byte flags) {
		trans = TransmissionType.values()[(flags & TRANS_MASK)];
		tos = TrafficClass.values()[((flags & TOS_MASK) >> TOS_SHIFT)];
	}

	private byte getFlags() {
		return (byte)(((byte)trans.ordinal() & TRANS_MASK) | (((byte)tos.ordinal() << TOS_SHIFT) & TOS_MASK));
	}

	@Override
	public ByteBuffer[] toByteBuffers() {
		ByteBuffer header = ByteBuffer.allocate(27);
		header.put((byte)1);
		header.put(version);
		header.put(getFlags());
		srcId.toByteBuffer(header);
		dstId.toByteBuffer(header);
		header.putInt(data.data.length);
		header.flip();
		ByteBuffer dat = ByteBuffer.wrap(data.data);
		return new ByteBuffer[]{header, dat};
	}

	@Override
	public String toString() {
		return "DATA " + srcId + "->" + dstId + " (" + trans + "/" + tos + ")";
	}
}
