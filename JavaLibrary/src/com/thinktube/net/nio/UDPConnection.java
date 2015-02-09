package com.thinktube.net.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import com.thinktube.net.NetworkInterfaces.NetIf;

public class UDPConnection implements NIOConnection, PktConnection {
	private final static Logger LOG = Logger.getLogger(UDPConnection.class.getSimpleName());
	private final static int MAX_QUEUE_LENGTH = 100;
	private final PktConnectionCallback cb;
	private final DatagramChannel chan;
	private final NIOSelectorThread selector;
	private InetSocketAddress remote;

	/* Note: We use allocate instead of allocateDirect to work around an Android 4.0 ICS bug:
	 * Ref: http://stackoverflow.com/questions/22063681/datagramchannel-receive-causes-an-indexoutofboundsexception
	 *      https://github.com/netty/netty/issues/1079
	 */
	private ByteBuffer buf = ByteBuffer.allocate(1500);

	private Queue<SendPkt> sendQueue = new LinkedBlockingQueue<SendPkt>(MAX_QUEUE_LENGTH);
	private InetAddress filterIP;
	private NetIf netIf;
	private int tos;

	/* note: we need to keep the ByteBuffers belonging to one packet together
	 * in order to guarantee that they will be delivered as one packet */
	private static class SendPkt {
		ByteBuffer[] data;
	}

	public UDPConnection(PktConnectionCallback cb, DatagramChannel chan, NIOSelectorThread server) {
		this.cb = cb;
		this.chan = chan;
		this.selector = server;
	}

	@Override
	public void handleRead() {
		/*
		 * A UDP receiver from NIOFactory.requestUDPReceiver() is not
		 * "connected", which means it does not have a fixed endpoint - we use
		 * it to listen to any UDP packet on port "X", no matter which source
		 * IP.
		 * 
		 * read() as used in the TCP connection can only work on connected
		 * channels so we cast back to DatagramChannel and use receive() below.
		 * That way we also get the sender IP address.
		 */
		try {
			InetSocketAddress src = (InetSocketAddress) chan.receive(buf);

			/*
			 * broadcast receivers will also receive packets sent by ourself so we
			 * provide a possibility to filter here
			 */
			//LOG.info("=== received on " + this);
			if (filterIP == null || !src.getAddress().equals(filterIP)) {
				buf.flip();
				cb.handleReceive(buf, src.getAddress(), this);
			} else {
				//LOG.info("=== filtered " + filterIP);
			}
			buf.clear();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean handleWrite() {
		int num = 0;
		try {
			while (!sendQueue.isEmpty()) {
				SendPkt sd = sendQueue.peek();
				chan.write(sd.data);
				//LOG.info("=== sent " + len + " on " + this);
				for (int i=0; i<sd.data.length; i++) {
					if (sd.data[i].remaining() > 0) {
						/* if some data is remaining, sending is not finished */
						LOG.fine("sent " + num + " packets, remaining: " + sendQueue.size());
						return false;
					}
				}
				sendQueue.remove();
				num++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		LOG.fine("sent " + num + " packets, remaining: " + sendQueue.size());
		return true; /* send finished */
	}

	@Override
	public boolean hasData() {
		return !sendQueue.isEmpty();
	}

	@Override
	public boolean send(ByteBuffer[] bufs) throws ClosedChannelException {
		if (!chan.isOpen()) {
			throw new ClosedChannelException();
		}

		SendPkt sd = new SendPkt();

		/*
		 * Here we duplicate the ByteBuffer[] as read only buffers. The buffers
		 * share the same data, but are independent otherwise.
		 * We do this in order to be able to send the same data on multiple
		 * connections (esp. broadcast data)
		 */
		sd.data = new ByteBuffer[bufs.length];
		for (int i=0; i<bufs.length; i++)
			sd.data[i] = bufs[i].asReadOnlyBuffer();

		if (!sendQueue.offer(sd)) {
			LOG.severe("send queue full, dropped data! " + this);
			return false;
		}

		/* always request send (TODO: optimize?) */
		selector.requestOps(this, SelectionKey.OP_WRITE);
		return true;
	}

	@Override
	public void close() {
		try {
			LOG.info("close " + this);
			chan.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public InetAddress getRemoteIP() {
		return chan.socket().getInetAddress();
	}

	@Override
	public InetAddress getLocalIP() {
		return chan.socket().getLocalAddress();
	}

	@Override
	public String toString() {
		if (chan.isOpen() && chan.socket().getInetAddress() != null)
			return "UDPConnection " + chan.socket().getLocalAddress() + " -> " + chan.socket().getInetAddress() + " TOS:0x" + Integer.toHexString(tos) + " Q:" + sendQueue.size() + " (" + chan + ")";
		else if (chan.isOpen())
			return "UDPConnection " + chan.socket().getLocalAddress() + " ToS:0x" + Integer.toHexString(tos) + " (" + chan + ")";
		else
			return "UDPConnection TOS:0x" + Integer.toHexString(tos) + " (" + chan + ")";
	}

	/**
	 * This is useful on UDP Broadcast receivers where we'd otherwise receive
	 * our own sent packets
	 */
	public void setReceiveFilterIP(InetAddress ip) {
		filterIP = ip;
	}

	@Override
	public void setNetIf(NetIf ni) {
		netIf = ni;
	}

	@Override
	public NetIf getNetIf() {
		return netIf;
	}

	public void setRemoteISA(InetSocketAddress isa) {
		remote = isa;
	}

	public InetSocketAddress getRemoteISA() {
		return remote;
	}

	@Override
	public SelectableChannel getChannel() {
		return chan;
	}

	public void setTOS(int tos) {
		this.tos = tos;
	}

	public static UDPConnection createReceiver(NIOSelectorThread selector, InetSocketAddress isa, PktConnectionCallback cb) throws IOException {
		DatagramChannel chan = DatagramChannel.open();
		chan.configureBlocking(false);
		chan.socket().setReuseAddress(true);
		chan.socket().bind(isa);

		UDPConnection conn = new UDPConnection(cb, chan, selector);
		selector.requestRegister(conn, SelectionKey.OP_READ);
		// this calls selector.wakeup();
		return conn;
	}

	public static UDPConnection createSender(NIOSelectorThread selector, InetSocketAddress isa, int tos, boolean broadcast, PktConnectionCallback cb) throws IOException {
		DatagramChannel chan = DatagramChannel.open();
		chan.configureBlocking(false);
		//chan.socket().setReuseAddress(true);

		if (tos != 0) {
			chan.socket().setTrafficClass(tos);
		}

		/* In order to "connect" to the broadcast address we have to setBroadcast() first */
		if (broadcast) {
			chan.socket().setBroadcast(true);
		}

		/*
		 * Note that we can't call connect() here because on Android this can
		 * cause NetworkOnMainThreadException, even though all networking is
		 * non-blocking! Our solution is to connect() in the NIO Selector thread
		 * when we handle the registration with OP_CONNECT.
		 *
		 * Note that OP_CONNECT is not valid for UDP, but we just re-use it for
		 * that purpose and set it to OP_READ in processChangeRequests().
		 */

		UDPConnection conn = new UDPConnection(cb, chan, selector);
		conn.setRemoteISA(isa);
		conn.setTOS(tos);
		selector.requestRegister(conn, SelectionKey.OP_CONNECT);
		// this calls selector.wakeup();
		return conn;
	}
}
