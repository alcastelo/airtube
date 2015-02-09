package com.thinktube.airtube.routing;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import com.thinktube.airtube.AirTube;
import com.thinktube.airtube.DataTransmit;
import com.thinktube.airtube.TrafficClass;
import com.thinktube.airtube.TransmissionType;
import com.thinktube.net.nio.PktConnection;

public class Traceroute implements DataTransmit.PacketHandler {
	private final static Logger LOG = Logger.getLogger(Traceroute.class.getSimpleName());
	public static final byte TRACE_PKT_TYPE = 9;
	//                        TRACE_TYPE_REQUEST = 0x0; //0000 0000
	private static final byte TRACE_TYPE_REPLY   = 0x1; //0000 0001
	private static final byte TRACE_TYPE_PING    = 0x0; //0000 000x
	private static final byte TRACE_TYPE_ROUTE   = 0x2; //0000 001x
	private static final byte TRACE_DEFAULT_TTL = 20;
	private static final int TRACE_MIN_PKT_SIZE = 19;
	private final long myDeviceId;

	private static class TraceInfo {
		long dst;
		long timeStarted;
		int interval;
		byte type; // ping or trace
		Timer timer;
	}

	private TraceInfo curTrace; // only one can be pending for now

	public Traceroute(long deviceId) {
		this.myDeviceId = deviceId;
	}

	/*** Packet format and handling ***/

	/*
	 * The packet format is simply:
	 *
	 * ===================================================
	 * 1 byte | PKT_TYPE | =99 (checked by DataTransmit)
	 * ---------------------------------------------------
	 * 1 byte | TYPE     | ping/traceroute request/reply
	 * 1 byte | TTL      | time to live
	 * 8 byte | DST      | deviceId (long)
	 * 8 byte | SRC      | deviceId (long)
	 * --- (only Traceroute) -----------------------------
	 * 8 byte | HOP      | deviceId (long)
	 * ... (multiple) ...
	 * ===================================================
	 */
	private void send(long dst, byte type) {
		ByteBuffer buf = ByteBuffer.allocate(TRACE_MIN_PKT_SIZE);
		buf.put(TRACE_PKT_TYPE);
		buf.put(type);
		buf.put(TRACE_DEFAULT_TTL);
		buf.putLong(dst);
		buf.putLong(myDeviceId);  // src
		buf.flip();

		LOG.info("Start " + (isTraceRoute(type) ? "Traceroute" : "Ping") + " to " + Long.toHexString(dst) + ": " + buf);
		DataTransmit.getInstance().send(buf, dst, TransmissionType.UDP, TrafficClass.NORMAL, false);
	}

	@Override
	public void receive(ByteBuffer buf, InetAddress unused_from, PktConnection unused_conn) {
		// standard fields:
		byte type = buf.get();
		byte ttl = buf.get(); ttl--;
		long dst = buf.getLong();
		long src = buf.getLong();

		if (dst != myDeviceId && ttl > 0) {
			forward(dst, type, ttl, buf);
		} else if (dst == myDeviceId && (type & TRACE_TYPE_REPLY) == 0 && ttl > 0) {
			sendBack(dst, src, type, ttl, buf);
		} else if (dst == myDeviceId && (type & TRACE_TYPE_REPLY) == TRACE_TYPE_REPLY) {
			traceResult(src, buf);
		} else if (ttl <= 0) {
			LOG.warning("DROP " + (isTraceRoute(type) ? "Traceroute" : "Ping") + " packet TTL expired");
		}
	}

	private void forward(long dst, byte type, byte ttl, ByteBuffer buf) {
		// here we copy the original packet and just decrement the ttl
		// for a route trace we add ourself to the list of relays
		ByteBuffer buf2 = ByteBuffer.allocate(isTraceRoute(type) ? buf.limit() + 8 : TRACE_MIN_PKT_SIZE);
		buf.put(2, ttl); // update TTL
		buf.rewind();
		buf2.put(buf);
		if (isTraceRoute(type)) {
			buf2.putLong(myDeviceId);
		}
		buf2.flip();
		LOG.info("Forward " + (isTraceRoute(type) ? "Traceroute" : "Ping") + " to " + Long.toHexString(dst) + " TTL " + ttl + ": " + buf2);
		DataTransmit.getInstance().send(new ByteBuffer[] {buf2}, dst, TransmissionType.UDP, TrafficClass.NORMAL);
	}

	private void sendBack(long dst, long src, byte type, byte ttl, ByteBuffer buf) {
		// construct new packet with type REPLY and swapped src and dst
		ByteBuffer out = ByteBuffer.allocate(isTraceRoute(type) ? buf.limit() + 8 : TRACE_MIN_PKT_SIZE);
		out.put(TRACE_PKT_TYPE);
		out.put((byte)(type | TRACE_TYPE_REPLY));
		out.put(ttl);
		out.putLong(src); // packet dst
		out.putLong(dst); // packet src
		if (isTraceRoute(type)) {
			// copy relay list + add myself
			out.put(buf);
			out.putLong(myDeviceId);
		}
		out.flip();
		LOG.info("Reply " + (isTraceRoute(type) ? "Traceroute" : "Ping") + " back to " + Long.toHexString(src) + ": " + out);
		DataTransmit.getInstance().send(new ByteBuffer[] {out}, src, TransmissionType.UDP, TrafficClass.NORMAL);
	}

	private String[] readHops(ByteBuffer buf) {
		ArrayList<String> hops = new ArrayList<String>();
		while (buf.remaining() >= 8) {
			long hop = buf.getLong();
			hops.add(Long.toHexString(hop));
		}
		return hops.toArray(new String[hops.size()]);
	}

	/*** Management stuff ***/

	public synchronized void startTrace(long dst, int type, int interval) {
		curTrace = new TraceInfo();
		curTrace.dst = dst;
		curTrace.interval = interval;
		curTrace.type = type == 0 ? TRACE_TYPE_PING : TRACE_TYPE_ROUTE;
		curTrace.timeStarted = System.currentTimeMillis();
		send(dst, curTrace.type);
	}

	private synchronized void traceResult(long src, ByteBuffer buf) {
		if (curTrace == null) {
			LOG.warning("Ignoring unknown trace result");
			return;
		}

		String[] s = new String[1];
		if (curTrace.type == TRACE_TYPE_PING) {
			s[0] = Long.toHexString(src) + " " + (System.currentTimeMillis() - curTrace.timeStarted) + "ms";
		} else if (curTrace.type == TRACE_TYPE_ROUTE) {
			s = readHops(buf);
		}
		AirTube.getMonitor().traceRouteResult(s);

		/* re-schedule if necessary */
		if (curTrace.interval == 0) {
			curTrace = null;
		} else {
			LOG.info("Scheduling trace in " + curTrace.interval);
			curTrace.timer = new Timer();
			curTrace.timer.schedule(new TimerTask() {
				@Override
				public void run() {
					synchronized (Traceroute.this) {
						curTrace.timeStarted = System.currentTimeMillis();
						send(curTrace.dst, curTrace.type);
					}
				}
			}, curTrace.interval * 1000);
		}
	}

	public synchronized void stopTrace() {
		if (curTrace == null) {
			LOG.warning("No trace pending");
			return;
		}

		if (curTrace.interval != 0 && curTrace.timer != null) {
			curTrace.timer.cancel();
			curTrace.timer = null;
		}
		curTrace = null;
	}

	private boolean isTraceRoute(byte type) {
		return ((type & TRACE_TYPE_ROUTE) != 0);
	}
}
