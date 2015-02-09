package com.thinktube.net.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import com.thinktube.net.NetworkInterfaces.NetIf;

public class TCPConnection implements NIOConnection, PktConnection {
	private final static Logger LOG = Logger.getLogger(TCPConnection.class.getSimpleName());
	private final static int MAX_QUEUE_LENGTH = 100;
	private static final long RECONNECT_TIME_MS = 1000; // 1 sec
	private final PktConnectionCallback cb;
	private SocketChannel chan;
	private final NIOSelectorThread selector;
	private ByteBuffer readBufLen = ByteBuffer.allocate(2);
	private ByteBuffer readBufData = ByteBuffer.allocateDirect(1500);
	private short readExpectedLen = 0;
	private Queue<SendPkt> sendQueue = new LinkedBlockingQueue<SendPkt>(MAX_QUEUE_LENGTH);
	/*
	 * remote is needed to connect to the destination, but is also used to
	 * distinguish "client connections" and "active" outgoing connections.
	 * "client connections" are clients which connected to our server socket,
	 * and we accept()ed them. "active" connections are connections we
	 * initiated and will try to reconnect.
	 */
	private InetSocketAddress remote;
	private long nextConnectRetryTime = 0;
	private int tos;
	private NetIf netIf;

	private static class SendPkt {
		/* len is the first element in the array */
		ByteBuffer[] data;
	}

	public TCPConnection(PktConnectionCallback cb, SocketChannel chan, NIOSelectorThread selector) {
		this.cb = cb;
		this.chan = chan;
		this.selector = selector;
	}

	@Override
	public void handleRead() {
		int c;
		try {
			if (readExpectedLen == 0) {
				c = chan.read(readBufLen);
				if (c == -1) { // EOF, socket closed on other side
					LOG.info("EOF while read len " + this);
					chan.close();
					scheduleReconnect();
					return;
				}
				if (readBufLen.position() == 2) {
					readBufLen.flip();
					readExpectedLen = readBufLen.getShort();
					readBufLen.clear();
					if (readExpectedLen > 0 && readExpectedLen <= readBufData.capacity()) {
						readBufData.limit(readExpectedLen);
					} else {
						LOG.warning("read invalid length of " + readExpectedLen);
						return;
					}
				}
			}

			if (readExpectedLen > 0) {
				c = chan.read(readBufData);
				if (readBufData.position() == readExpectedLen) {
					readBufData.flip();
					cb.handleReceive(readBufData, chan.socket().getInetAddress(), this);
					readBufData.clear();
					readExpectedLen = 0;
					return;
				}
				if (c == -1) { // EOF, socket closed
					LOG.info("EOF while read data " + this);
					chan.close();
					scheduleReconnect();
					return;
				}
			}
		} catch (IOException ioe) {
			LOG.info("IO Exception while read " + this);
			scheduleReconnect();
		}
	}

	@Override
	public boolean handleWrite() {
		int num = 0;
		try {
			while (!sendQueue.isEmpty()) {
				SendPkt sd = sendQueue.peek();
				chan.write(sd.data);
				for (int i = 0; i < sd.data.length; i++) {
					if (sd.data[i].remaining() > 0) {
						/* if some data is remaining, sending is not finished */
						LOG.fine("sent " + num + " packets, remaining: " + sendQueue.size());
						return false;
					}
				}
				sendQueue.remove();
				num++;
			}
		} catch (IOException ioe) {
			LOG.info("IO Exception while write " + this);
			scheduleReconnect();
		} catch (NotYetConnectedException nyce) {
			LOG.info("No yet connected Exception while write " + this);
			scheduleReconnect();
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
		if (!chan.isOpen() && remote == null) {
			/*
			 * this is a "client connection" and we can not reconnect on
			 * behalf of the client
			 */
			throw new ClosedChannelException();
		}
		/* otherwise we just queue the packet and hope we will be able to reconnect */

		short len=0;
		SendPkt sd = new SendPkt();
		sd.data = new ByteBuffer[bufs.length + 1];
		sd.data[0] = ByteBuffer.allocate(2);

		for (int i=0; i<bufs.length; i++) {
			len += bufs[i].limit();
			/*
			 * Here we duplicate the ByteBuffer[] as read only buffers. The buffers
			 * share the same data, but are independent otherwise.
			 * We do this in order to be able to send the same data on multiple
			 * connections
			 */
			sd.data[i+1] = bufs[i].asReadOnlyBuffer();
		}
		sd.data[0].putShort(len);
		sd.data[0].flip();
		if (!sendQueue.offer(sd)) {
			LOG.severe("send queue full, dropped data! " + this);
			return false;
		}

		/* we can only request send if we are already connected,
		 * otherwise we have to wait until we are connected */
		if (chan.isConnected())
			selector.requestOps(this, SelectionKey.OP_WRITE);
		else if (chan.isConnectionPending())
			LOG.info("data queued but not connected yet, Q:" + sendQueue.size());
		else {
			LOG.info("send needs reconnect, Q:" + sendQueue.size());
			reconnect();
		}

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
		return "TCPConnection " + chan.socket().getLocalAddress() + ":" + chan.socket().getLocalPort()
				+ " -> " + chan.socket().getInetAddress() + ":" + chan.socket().getPort()
				+ " ToS:0x" + Integer.toHexString(tos) + " Q:" + sendQueue.size();
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

	protected void scheduleReconnect() {
		if (remote != null && sendQueue.size() > 0) {
			LOG.fine("schedule reconnect for " + this);
			nextConnectRetryTime = System.currentTimeMillis() + RECONNECT_TIME_MS;
		} else if (remote == null) {
			LOG.fine("not reconnecting client " + this);
		}
	}

	protected boolean needsReconnect() {
		return (nextConnectRetryTime != 0);
	}

	protected boolean readyToReconnect() {
		return (nextConnectRetryTime != 0 && nextConnectRetryTime < System.currentTimeMillis());
	}

	protected void reconnect() {
		LOG.info("reconnect " + this);
		nextConnectRetryTime = 0;

		//TODO: We duplicate some code from below create() here
		try {
			chan = SocketChannel.open();
			chan.configureBlocking(false);

			if (tos != 0) {
				chan.socket().setTrafficClass(tos);
			}

			selector.requestRegister(this, SelectionKey.OP_CONNECT);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setTOS(int tos) {
		this.tos = tos;
	}

	@Override
	public void setNetIf(NetIf ni) {
		netIf = ni;
	}

	@Override
	public NetIf getNetIf() {
		return netIf;
	}

	public static TCPConnection create(NIOSelectorThread selector, InetSocketAddress isa, int tos, PktConnectionCallback cb) throws IOException {
		SocketChannel chan = SocketChannel.open();
		chan.configureBlocking(false);

		if (tos != 0) {
			chan.socket().setTrafficClass(tos);
		}

		/*
		 * Note that we can't call connect() here because on Android this can
		 * cause NetworkOnMainThreadException, even though all networking is
		 * non-blocking! Our solution is to connect() in the NIO Selector thread
		 * when we handle the registration with OP_CONNECT.
		 */

		TCPConnection conn = new TCPConnection(cb, chan, selector);
		conn.setRemoteISA(isa);
		conn.setTOS(tos);
		selector.requestRegister(conn, SelectionKey.OP_CONNECT);
		// this calls selector.wakeup();
		return conn;
	}
}
