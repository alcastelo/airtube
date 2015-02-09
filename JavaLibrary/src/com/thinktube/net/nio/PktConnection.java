package com.thinktube.net.nio;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import com.thinktube.net.NetworkInterfaces.NetIf;

/** This is the interface for users */
public interface PktConnection {

	boolean send(ByteBuffer[] bufs) throws ClosedChannelException;

	void close();
	
	InetAddress getRemoteIP();

	InetAddress getLocalIP();

	public void setNetIf(NetIf ni);

	public NetIf getNetIf();

}
