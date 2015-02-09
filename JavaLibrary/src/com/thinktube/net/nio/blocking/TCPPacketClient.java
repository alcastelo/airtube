package com.thinktube.net.nio.blocking;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class TCPPacketClient {
	protected SocketChannel channel;

	public TCPPacketClient(InetAddress ip, int port) throws IOException {
		channel = SocketChannel.open();
		channel.connect(new InetSocketAddress(ip, port));
	}

	public TCPPacketClient(String ip, int port) throws IOException {
		channel = SocketChannel.open();
		channel.connect(new InetSocketAddress(ip, port));
	}

	public void close() {
		if (channel != null) {
			try {
				channel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void setTrafficClass(int val) {
		try {
			channel.socket().setTrafficClass(val);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	public void send(ByteBuffer buf) throws IOException {
		ByteBuffer lenb = ByteBuffer.allocate(2);
		lenb.putShort((short) buf.limit());
		lenb.flip();
		channel.write(lenb);
		channel.write(buf);
	}

	public void send(ByteBuffer[] bufs) throws IOException {
		short len=0;
		ByteBuffer lenb = ByteBuffer.allocate(2);

		for (int i=0; i<bufs.length; i++)
			len+=bufs[i].limit();

		lenb.putShort(len);
		lenb.flip();
		channel.write(lenb);
		channel.write(bufs);
	}

	public ByteBuffer receive() throws IOException {
		ByteBuffer lenb = ByteBuffer.allocate(2);
		channel.read(lenb);
		lenb.flip();
		short expectedLen = lenb.getShort();
		ByteBuffer data = ByteBuffer.allocate(expectedLen);
		channel.read(data);
		data.flip();
		return data;
	}
}
