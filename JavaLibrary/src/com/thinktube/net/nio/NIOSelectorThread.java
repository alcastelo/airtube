package com.thinktube.net.nio;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

public class NIOSelectorThread implements Runnable {
	private final static Logger LOG = Logger.getLogger(NIOSelectorThread.class.getSimpleName());
	private volatile boolean running = true;
	private Selector selector;
	private Queue<ChangeRequest> requestQueue = new LinkedBlockingQueue<ChangeRequest>();
	private List<NIOConnection> registeredConnections = new LinkedList<NIOConnection>();

	private static class ChangeRequest {
		public enum Type { REGISTER, CHANGEOPS };
		public final Type request;
		public int ops;
		public final NIOConnection conn;

		public ChangeRequest(NIOConnection conn, Type req, int ops) {
			this.request = req;
			this.ops = ops;
			this.conn = conn;
		}
	}

	public NIOSelectorThread() {
	}

	@Override
	public void run() {
		Iterator<SelectionKey> keys;
		SelectionKey key;

		try {
			selector = Selector.open();
		} catch (IOException e) {
			LOG.severe("Can not open selector!");
			e.printStackTrace();
			return;
		}

		LOG.info("NIO selector running...");

		while (running) {
			try {
				/* process change requests which may have resulted from the operations below */
				processChangeRequests();

				if (selector.select() == 0) {
					continue;
				}

				keys = selector.selectedKeys().iterator();
				while (keys.hasNext()) {
					key = keys.next();
					keys.remove();

					if (!key.isValid()) {
						continue;
					}

					if (key.isAcceptable()) {
						accept(key);
					} else if (key.isConnectable()) {
						finishConnect(key);
					} else if (key.isReadable()) {
						read(key);
					} else if (key.isWritable()) {
						write(key);
					}

					doConnectionHouseKeeping();

				}
			} catch (Exception e) {
				if (running) {
					LOG.severe("Exception caught in NIO selector!");
					e.printStackTrace();
				}
				// else the socket was closed by stopRunning and this is OK
			}
		}

		LOG.info("NIO selector finished...");
	}

	private void accept(SelectionKey key) throws IOException {
		// must be a TCP server socket channel
		ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
		TCPServerConnection serverConn = (TCPServerConnection) key.attachment();
		SocketChannel clientChannel = serverChannel.accept();
		clientChannel.configureBlocking(false);
		NIOConnection conn = new TCPConnection(serverConn.getCallback(), clientChannel, this);
		LOG.fine("accepted client " + conn);
		clientChannel.register(selector, SelectionKey.OP_READ, conn);
		registeredConnections.add(conn);
	}

	private void finishConnect(SelectionKey key) throws IOException {
		// must be an active outgoing TCP connection we initiated before
		LOG.fine("finish connect");
		SocketChannel clientChannel = (SocketChannel) key.channel();
		NIOConnection conn = (NIOConnection) key.attachment();
		try {
			clientChannel.finishConnect();
			key.interestOps(conn.hasData() ? SelectionKey.OP_WRITE : SelectionKey.OP_READ);
		} catch (SocketException se) { // superclass of ConnectException, NoRouteToHostException, PortUnreachableException
			LOG.info("connect failed " + conn);
			key.cancel();
			((TCPConnection)conn).scheduleReconnect();
		} catch (SocketTimeoutException ste) {
			LOG.info("connect timed out " + conn);
			key.cancel();
			((TCPConnection)conn).scheduleReconnect();
		}
	}

	private void read(SelectionKey key) {
		NIOConnection conn = (NIOConnection) key.attachment();
		LOG.fine("read on " + conn);
		conn.handleRead();
		if (!conn.getChannel().isOpen()) {
			LOG.fine("cancel key " + conn);
			key.cancel();
			// we remove closed channels in the housekeeping function
		}
	}

	private void write(SelectionKey key) {
		NIOConnection conn = (NIOConnection) key.attachment();
		LOG.fine("write on " + conn);
		boolean ret = conn.handleWrite();
		if (ret) {
			LOG.fine("write finished");
			key.interestOps(SelectionKey.OP_READ);
		}
	}

	/**
	 * tries to reconnect broken or not yet connected TCP connections.
	 */
	private void doConnectionHouseKeeping() {
		Iterator<NIOConnection> it = registeredConnections.iterator();
		while (it.hasNext()) {
			NIOConnection nc = it.next();
			if (!nc.getChannel().isOpen()) {
				if (nc instanceof TCPConnection) {
					/* we can only reconnect for TCP */
					TCPConnection tc = (TCPConnection)nc;
					if (tc.readyToReconnect()) {
						// we remove here because reconnect() will add it again...
						it.remove();
						tc.reconnect();
					} else if (!tc.needsReconnect()) {
						/* client channels may be just closed and we don't reconnect them */
						LOG.fine("removing closed " + nc);
						it.remove();
					}
				} else {
					LOG.fine("removing closed " + nc);
					it.remove();
				}
			}

			//TODO: time out long time unused connections?
			//TODO: check send queue sizes as well?
		}
	}

	private void processChangeRequests() throws IOException {
		while (!requestQueue.isEmpty()) {
			ChangeRequest change = requestQueue.remove();
			switch (change.request) {

			case CHANGEOPS:
				LOG.fine("processing change OP " + change.ops + " for " + change.conn);
				SelectionKey key = change.conn.getChannel().keyFor(selector);
				if (key == null) {
					LOG.warning("channel not registered");
					continue;
				}
				key.interestOps(change.ops);
				break;

			case REGISTER:
				LOG.fine("register " + change.conn);
				/*
				 * note, we use the OP_CONNECT also for UDP to connect() the channel here.
				 * as this is not a valid OP for UDP we change it to OP_READ when we actually
				 * register the channel with the selector
				 */
				if (change.ops == SelectionKey.OP_CONNECT) {
					LOG.fine("connect " + change.conn);
					if (change.conn instanceof TCPConnection) {
						((SocketChannel)change.conn.getChannel()).connect(change.conn.getRemoteISA());
					} else if (change.conn instanceof UDPConnection) {
						((DatagramChannel)change.conn.getChannel()).connect(change.conn.getRemoteISA());
						change.ops = SelectionKey.OP_READ;
					}
				}

				change.conn.getChannel().register(selector, change.ops, change.conn);
				registeredConnections.add((NIOConnection)change.conn);
				break;
			}
		}
	}

	public void requestOps(NIOConnection conn, int ops) {
		ChangeRequest req = new ChangeRequest(conn, ChangeRequest.Type.CHANGEOPS, ops);
		requestQueue.add(req);
		if (selector != null) {
			selector.wakeup();
		}
	}

	public void requestRegister(NIOConnection conn, int ops) {
		ChangeRequest req = new ChangeRequest(conn, ChangeRequest.Type.REGISTER, ops);
		requestQueue.add(req);
		if (selector != null) {
			selector.wakeup();
		}
	}

	public synchronized void start() {
		running = true;
		new Thread(this, "NIO Selector").start();
	}

	public synchronized void stop() {
		running = false;
		LOG.info("stopping");

		try {
			for (NIOConnection nc : registeredConnections) {
				SelectableChannel ch = nc.getChannel();
				if (ch.isOpen()) {
					LOG.fine("closing " + ch);
					ch.close();
				} else {
					LOG.fine("already closed " + ch);
				}
			}
			registeredConnections.clear();
			if (selector != null) {
				selector.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
