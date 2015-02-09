package com.thinktube.airtube.routing;

public class Route {
	//private static final Logger LOG = Logger.getLogger(Route.class.getSimpleName());
	public final long deviceId;
	public final long lastHop; /* source tracing for DV improvement, this is deviceID */
	public final long nextHop;
	/* we allow only to change/update these: */
	public Metric metric;
	public short seqNo;
	private long lastTime;
	private int age;   /* tick by each MS from the birth at the origin node */

	private enum State {
		UP, DOWN
	};
	private State state;

	public Route(long deviceId, long lastHop, long nextHop, Metric metric, short seqNo, int age) {
		this.state = State.UP;
		this.deviceId = deviceId;
		this.nextHop = nextHop;
		this.lastHop = lastHop;
		this.metric = metric;
		this.seqNo = seqNo;
		this.lastTime = System.currentTimeMillis();
		this.age = age;
		if (!metric.isInfinite()) {
			stateUP();
		} else {
			stateDOWN();
		}
	}

	public void update(Metric metric, short seqNo, int age) {
		/*
		 * FIXME: should be careful not to allow decrement seqNo, call might do
		 * misbehavior or we might allow seqNo decrement if the caller guarantee
		 * it is loop free
		 */
		if (!metric.isInfinite()) {
			stateUP();
			this.seqNo = seqNo;
			this.metric = metric;
			this.age = age;
			this.lastTime = System.currentTimeMillis();
		} else {
			stateDOWN();
			/*
			 * metric is not updated to METRIC_INFINITE, rather keep the old
			 * metric value for later reference
			 * 
			 * we MUST not update the timestamp when notified DOWN state,
			 * otherwise the route will not expire forever
			 */
		}
	}

	public boolean isNbrRoute() {
		return (deviceId == nextHop);
	}

	public void stateUP() {
		state = State.UP;
	}

	public void stateDOWN() {
		state = State.DOWN;
	}

	public boolean isUP() {
		return (state == State.UP);
	}

	public boolean isDOWN() {
		return (state == State.DOWN);
	}

	@Override
	public String toString() {
		return "Route [to: " + Long.toHexString(deviceId) + " " + state + " next: " + Long.toHexString(nextHop) +
				" last: " + Long.toHexString(lastHop) + " metric: " + metric + " (" + seqNo + "/" + getCurrentAge() + "ms)]";
	}

	public int getCurrentAge() {
		return age + (int)(System.currentTimeMillis() - lastTime);
	}

	public long getLastTime() {
		return lastTime;
	}
}
